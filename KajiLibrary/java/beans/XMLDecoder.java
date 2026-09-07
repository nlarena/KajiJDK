package java.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

// XMLEncoder's counterpart: it reads the document and returns the objects it describes.
//
// The graph is not "deserialized" -- it is **rebuilt by executing**. Each element of the document is
// a call (`Statement`/`Expression`) and reading is running them in order: `<object class="Foo">` is
// `new Foo()`, `<void property="x">` is `setX(...)`, `<object idref="f0"/>` is "the same object as
// before". That is why a bean file does not depend on private fields nor on a serialVersionUID: it
// depends on the class still having the constructor and the setters the file names.
//
// The `<java>` element's value is **this decoder**. It sounds odd until one looks at what XMLEncoder
// writes for the owner: `<void id="X0" property="owner"/>`, which is `getOwner()` on `<java>`'s
// value. Putting the decoder there makes that call resolve to the owner handed to the constructor,
// which is exactly what the word means: "the object this document expects to be given when it is
// read". It is the same thing the JDK does.
//
// Reading happens whole and at once: the first `readObject()` parses the entire document, builds the
// graph and then hands the objects over one by one. It could not be lazy -- an `idref` may point at
// something defined further down inside the same root -- and the JDK's is not either. Once the list
// is exhausted, `readObject()` throws ArrayIndexOutOfBoundsException, which is how the JDK says
// "there are no more".
//
// ## Where it gets the XML from
//
// **There is no system SAX parser.** In this tree `org.xml.sax` are interfaces with no
// implementation at all: `XMLReaderFactory` has nobody to instantiate. So the parsing is done by
// `XmlParser`, this package's own parser, which reads the bean persistence dialect and nothing else
// --no DTD, no namespaces, no document entities. It is a declared limitation of the parser's scope,
// not of the format: everything XMLEncoder writes fits.
//
// On the constructor that receives an `org.xml.sax.InputSource`: both ways in which a source BRINGS
// the document are attended to --a byte stream or a Reader-- and, if it only brings a system id, it
// is opened as a file (accepting the `file:` prefix) and otherwise as a URL. A system id no
// `URLStreamHandler` knows how to open fails with IOException where the JDK would resolve it, and
// that is exactly what `java.net` lacks in this tree, not something this method decides.
public class XMLDecoder implements AutoCloseable {

    private final InputStream in;
    private final Reader reader;
    private final ClassLoader loader;

    private Object owner;
    private ExceptionListener exceptionListener;

    private Object[] objects;
    private int index;

    public XMLDecoder(InputStream in) {
        this(in, null, null, null);
    }

    public XMLDecoder(InputStream in, Object owner) {
        this(in, owner, null, null);
    }

    public XMLDecoder(InputStream in, Object owner, ExceptionListener exceptionListener) {
        this(in, owner, exceptionListener, null);
    }

    public XMLDecoder(InputStream in, Object owner, ExceptionListener exceptionListener,
            ClassLoader cl) {
        this.in = in;
        this.reader = null;
        this.owner = owner;
        this.exceptionListener = exceptionListener;
        this.loader = cl;
    }

    // A SAX source. Its content is asked for in the order the specification says it has to be
    // looked at: first the character stream, then the byte one, and only lastly the system id.
    public XMLDecoder(org.xml.sax.InputSource is) {
        if (is == null) {
            throw new IllegalArgumentException("input source is null");
        }
        this.owner = null;
        this.exceptionListener = null;
        this.loader = null;
        Reader r = is.getCharacterStream();
        InputStream s = r != null ? null : is.getByteStream();
        if (r == null && s == null) {
            s = openSystemId(is.getSystemId());
        }
        this.reader = r;
        this.in = s;
    }

    // `file:/x/y` and `/x/y` go straight to the file system; anything else is tried as a URL.
    // Opening it here and not in the first readObject is what lets the error come out as an
    // IllegalArgumentException from the constructor, which is where the caller expects it.
    private static InputStream openSystemId(String systemId) {
        if (systemId == null) {
            throw new IllegalArgumentException("input source carries neither a stream nor a system id");
        }
        try {
            if (systemId.startsWith("file:")) {
                String path = systemId.substring(5);
                while (path.startsWith("///")) {
                    path = path.substring(2);
                }
                if (path.startsWith("//")) {
                    path = path.substring(2);
                }
                return new java.io.FileInputStream(path);
            }
            if (systemId.indexOf(':') < 0) {
                return new java.io.FileInputStream(systemId);
            }
            return new java.net.URL(systemId).openStream();
        } catch (IOException ex) {
            throw new IllegalArgumentException("could not open " + systemId + ": " + ex);
        }
    }

    public void setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    // Never null: with none set by hand, the one that prints and carries on. It is the same promise
    // Encoder makes and the one the JDK documents.
    public ExceptionListener getExceptionListener() {
        return this.exceptionListener != null
            ? this.exceptionListener : Delegates.DEFAULT_LISTENER;
    }

    public void setOwner(Object owner) {
        this.owner = owner;
    }

    public Object getOwner() {
        return this.owner;
    }

    // The document's next object. Once they run out, ArrayIndexOutOfBoundsException: the JDK uses
    // that very exception as end of list, and whoever reads in a loop catches it to stop.
    public Object readObject() {
        if (this.objects == null) {
            this.parse();
        }
        return this.objects[this.index++];
    }

    // It closes the input. It parses first, because a document may have no top-level object at all
    // and consist only of calls on the owner: if closing did not parse, those calls would never
    // happen.
    public void close() {
        if (this.objects == null) {
            this.parse();
        }
        try {
            if (this.reader != null) {
                this.reader.close();
            }
            if (this.in != null) {
                this.in.close();
            }
        } catch (IOException e) {
            this.getExceptionListener().exceptionThrown(e);
        }
    }

    private void parse() {
        this.objects = new Object[0];
        if (this.in == null && this.reader == null) {
            return;
        }
        BeansHandler handler = new BeansHandler(this, this.exceptionListener, this.loader);
        try {
            XmlNode root = this.reader != null
                ? XmlParser.parseText(XmlParser.readAll(this.reader))
                : XmlParser.parseDocument(this.in);
            handler.startDocument();
            replay(root, handler);
            List<Object> l = handler.objects();
            this.objects = l.toArray(new Object[l.size()]);
        } catch (Exception e) {
            this.getExceptionListener().exceptionThrown(e);
        }
    }

    // The tree the parser returned, told to the handler as if a SAX parser had dictated it. It goes
    // through `content` and not through `children`/`text` because the order between the text and the
    // elements is information: inside a `<string>` it decides where each piece ends up.
    private static void replay(XmlNode node, BeansHandler handler) {
        handler.open(node.name, node.attributes);
        for (int i = 0; i < node.content.size(); i++) {
            Object child = node.content.get(i);
            if (child instanceof XmlNode) {
                replay((XmlNode) child, handler);
            } else {
                handler.text((String) child);
            }
        }
        handler.close();
    }

    // A SAX handler that builds the same graph this decoder builds. What the document does to the
    // `owner` from `<java>`'s level is what is observable from outside: the return type is
    // DefaultHandler and it has no way of handing over the top-level objects, and the JDK's handler
    // is in the same position --it lives in an internal package nobody exports.
    public static org.xml.sax.helpers.DefaultHandler createHandler(Object owner,
            ExceptionListener el, ClassLoader cl) {
        return new BeansHandler(owner, el, cl);
    }
}
