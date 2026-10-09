package org.xml.sax.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

// KajiLibrary's org.xml.sax.helpers.ParserAdapter -- a SAX1 parser with a SAX2 face.
//
// It implements XMLReader (SAX2) on the outside and DocumentHandler (SAX1) on the inside: it
// registers itself with the old parser it wraps, receives the old events and emits them again as
// new ones. It analyses nothing on its own; all the reading is done by the wrapped Parser.
//
// The whole reason for it is the one thing SAX1 does not do: namespaces. A SAX1 parser reports
// `<xsl:template match="/">` with the name "xsl:template" and an attribute list that still contains
// the xmlns declarations, and knows nothing of what "xsl" means. This adapter runs a
// NamespaceSupport in parallel and turns that into the SAX2 (uri, localName, qName) plus the
// startPrefixMapping/endPrefixMapping events.
//
// All the work is in startElement, and it is two passes over the attribute list on purpose. **The
// first pass attends only to the xmlns declarations**, because a declaration made on this element
// applies to the name of the element itself and to its other attributes; resolving any name before
// all the declarations are in would resolve it against the bindings of the parent. Only then does
// the second pass copy the real attributes with the bindings already final. Doing it in one single
// pass is the classic way of getting `<a xmlns:p="u" p:x="1">` wrong.
//
// Three SAX2 features are recognised, and only those three:
//
//   namespaces          (default true)   do the processing, to begin with
//   namespace-prefixes  (default false)  also pass the xmlns attributes up to SAX2
//   xmlns-uris          (default false)  and give those attributes the NSDECL namespace
//
// The first two cannot both be false --with neither, the name of an element would have neither a
// resolved form nor a raw one--, so switching one off switches the other on. None of them can be
// changed in the middle of the analysis; that throws SAXNotSupportedException instead of producing
// a document analysed under two different rules. No property is recognised: there is nothing
// underneath to ask.
//
// An undeclared prefix is reported to the ErrorHandler as a recoverable error, not thrown. For an
// *attribute* the adapter goes further and keeps the attribute, with an empty URI and the raw name
// in the slot of the local name, so that a misplaced prefix does not silently erase an attribute
// of the document. The errors of the second pass are collected and reported after the passes and
// not in the middle of the loop, so that the handler sees a consistent attribute list.
//
// COMPILATION NOTE, and it is not cosmetic -- the same one DefaultHandler.java carries: the
// `contentHandler` field and the signatures of `setContentHandler`/`getContentHandler` write
// `org.xml.sax.ContentHandler` with its full name even though the `import` above already brings
// it. It is not redundancy: it is the way round finding #530 of the report (the note said #466, the
// number it had before the renumbering). When the source of org/xml/sax/ContentHandler.java enters
// the SAME invocation as this file, our javac ignores the single-type import and resolves the
// simple name against java.net.ContentHandler, which exists in the tree and is an abstract class,
// not the interface. And as it does not check either that what goes in an `implements` is an
// interface (#467), out comes a .class that compiles, measures fine, runs on our VM and dies with
// IncompatibleClassChangeError on a real JVM. **Do not "clean" it to a simple name.**
public class ParserAdapter implements XMLReader, DocumentHandler {

    private static final String FEATURES = "http://xml.org/sax/features/";
    private static final String NAMESPACES = FEATURES + "namespaces";
    private static final String NAMESPACE_PREFIXES =
        FEATURES + "namespace-prefixes";
    private static final String XMLNS_URIs = FEATURES + "xmlns-uris";

    private NamespaceSupport nsSupport = new NamespaceSupport();
    private AttributeListAdapter attAdapter;

    private boolean parsing = false;
    private String nameParts[] = new String[3];

    private Parser parser = null;
    private AttributesImpl atts = null;

    // The feature flags. The default values are those of SAX2: process namespaces, hide the
    // declarations.
    private boolean namespaces = true;
    private boolean prefixes = false;
    private boolean uris = false;

    Locator locator;
    EntityResolver entityResolver = null;
    DTDHandler dtdHandler = null;
    org.xml.sax.ContentHandler contentHandler = null;
    ErrorHandler errorHandler = null;

    // It wraps whatever ParserFactory finds, that is the class the system property
    // `org.xml.sax.parser` names. Each way that can fail becomes a SAXException here, which is the
    // difference between the error conventions of SAX1 and SAX2.
    public ParserAdapter() throws SAXException {
        super();

        String driver = System.getProperty("org.xml.sax.parser");

        try {
            setup(ParserFactory.makeParser());
        } catch (ClassNotFoundException e1) {
            throw new SAXException("Cannot find SAX1 driver class "
                                   + driver, e1);
        } catch (IllegalAccessException e2) {
            throw new SAXException("SAX1 driver class " + driver
                                   + " found but cannot be loaded", e2);
        } catch (InstantiationException e3) {
            throw new SAXException("SAX1 driver class " + driver
                                   + " loaded but cannot be instantiated", e3);
        } catch (ClassCastException e4) {
            throw new SAXException("SAX1 driver class " + driver
                                   + " does not implement org.xml.sax.Parser");
        } catch (NullPointerException e5) {
            throw new SAXException("System property org.xml.sax.parser not"
                                   + " specified");
        }
    }

    public ParserAdapter(Parser parser) {
        super();
        setup(parser);
    }

    private void setup(Parser parser) {
        if (parser == null) {
            throw new NullPointerException("Parser argument must not be null");
        }
        this.parser = parser;
        atts = new AttributesImpl();
        nameParts = new String[3];
        attAdapter = new AttributeListAdapter();
    }

    ////////////////////////////////////////////////////////////////////
    // XMLReader: configuration
    ////////////////////////////////////////////////////////////////////

    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(NAMESPACES)) {
            checkNotParsing("feature", name);
            namespaces = value;
            // Both off means nothing: the caller would receive the names in neither of the two
            // forms.
            if (!namespaces && !prefixes) {
                prefixes = true;
            }
        } else if (name.equals(NAMESPACE_PREFIXES)) {
            checkNotParsing("feature", name);
            prefixes = value;
            if (!prefixes && !namespaces) {
                namespaces = true;
            }
        } else if (name.equals(XMLNS_URIs)) {
            checkNotParsing("feature", name);
            uris = value;
        } else {
            throw new SAXNotRecognizedException("Feature: " + name);
        }
    }

    public boolean getFeature(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(NAMESPACES)) {
            return namespaces;
        } else if (name.equals(NAMESPACE_PREFIXES)) {
            return prefixes;
        } else if (name.equals(XMLNS_URIs)) {
            return uris;
        } else {
            throw new SAXNotRecognizedException("Feature: " + name);
        }
    }

    // A SAX1 parser has no properties, so none can be recognised. Asserting otherwise would be
    // asserting something about the parser underneath that this class cannot know.
    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException("Property: " + name);
    }

    public Object getProperty(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException("Property: " + name);
    }

    // The handlers are kept here and setupParser() pushes them onto the parser at the moment of
    // analysing, except the document handler, which is always this adapter.
    public void setEntityResolver(EntityResolver resolver) {
        entityResolver = resolver;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void setDTDHandler(DTDHandler handler) {
        dtdHandler = handler;
    }

    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }

    public void setContentHandler(org.xml.sax.ContentHandler handler) {
        contentHandler = handler;
    }

    public org.xml.sax.ContentHandler getContentHandler() {
        return contentHandler;
    }

    public void setErrorHandler(ErrorHandler handler) {
        errorHandler = handler;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void parse(String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    // Reentry is rejected outright: the namespace stack and the attribute buffer are state of one
    // analysis, and a second parse on the same adapter would trample the first.
    public void parse(InputSource input) throws IOException, SAXException {
        if (parsing) {
            throw new SAXException("Parser is already in use");
        }
        setupParser();
        parsing = true;
        try {
            parser.parse(input);
        } finally {
            parsing = false;
        }
    }

    ////////////////////////////////////////////////////////////////////
    // DocumentHandler: the SAX1 events that arrive from the wrapped parser
    ////////////////////////////////////////////////////////////////////

    // It is kept besides being forwarded, because makeException() needs it to give the errors a
    // position.
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        if (contentHandler != null) {
            contentHandler.setDocumentLocator(locator);
        }
    }

    public void startDocument() throws SAXException {
        if (contentHandler != null) {
            contentHandler.startDocument();
        }
    }

    public void endDocument() throws SAXException {
        if (contentHandler != null) {
            contentHandler.endDocument();
        }
    }

    // The two-pass translation described in the comment of the class.
    public void startElement(String qName, AttributeList qAtts)
            throws SAXException {
        // Errors found while resolving attribute names, held back until the two passes finish so
        // that the handler never sees a half-built attribute list.
        List<SAXException> exceptions = null;

        // With namespace processing off there is nothing to resolve: the SAX1 list is passed
        // straight through behind an Attributes face, with empty uri and local name.
        if (!namespaces) {
            if (contentHandler != null) {
                attAdapter.setAttributeList(qAtts);
                contentHandler.startElement("", "", canon(qName),
                                            attAdapter);
            }
            return;
        }

        nsSupport.pushContext();
        int length = qAtts.getLength();

        // Pass one: only the declarations. Each and every one of them has to be in force before any
        // name of this element is resolved.
        for (int i = 0; i < length; i++) {
            String attQName = qAtts.getName(i);

            if (!attQName.startsWith("xmlns")) {
                continue;
            }

            String prefix;
            int n = attQName.indexOf(':');

            if (n == -1 && attQName.length() == 5) {
                // Exactly `xmlns`: the default namespace.
                prefix = "";
            } else if (n != 5) {
                // Something like `xmlnsfoo` or `xmlnsf:oo`: it starts with the five letters but is
                // not a declaration. The standard does not speak of these; SAX ignores them.
                continue;
            } else {
                // `xmlns:foo`.
                prefix = attQName.substring(n + 1);
            }

            String value = qAtts.getValue(i);
            if (!nsSupport.declarePrefix(prefix, value)) {
                // The prefix was "xml" or "xmlns", which cannot be redeclared.
                reportError("Illegal Namespace prefix: " + prefix);
                continue;
            }
            if (contentHandler != null) {
                contentHandler.startPrefixMapping(prefix, value);
            }
        }

        // Pass two: the attributes proper, resolved against the bindings that are now complete.
        atts.clear();
        for (int i = 0; i < length; i++) {
            String attQName = qAtts.getName(i);
            String type = qAtts.getType(i);
            String value = qAtts.getValue(i);

            if (attQName.startsWith("xmlns")) {
                String prefix;
                int n = attQName.indexOf(':');

                if (n == -1 && attQName.length() == 5) {
                    prefix = "";
                } else if (n != 5) {
                    // It was not a declaration after all; it goes on to be treated below as an
                    // ordinary attribute.
                    prefix = null;
                } else {
                    prefix = attQName.substring(n + 1);
                }

                if (prefix != null) {
                    // A real declaration. It reaches SAX2 only if the caller asked for it, and only
                    // then in the NSDECL namespace if they also asked for that.
                    if (prefixes) {
                        if (uris) {
                            atts.addAttribute(NamespaceSupport.NSDECL, prefix,
                                              canon(attQName), type, value);
                        } else {
                            atts.addAttribute("", "", canon(attQName),
                                              type, value);
                        }
                    }
                    continue;
                }
            }

            // An ordinary attribute.
            try {
                String attName[] = processName(attQName, true, true);
                atts.addAttribute(attName[0], attName[1], attName[2],
                                  type, value);
            } catch (SAXException e) {
                if (exceptions == null) {
                    exceptions = new ArrayList<SAXException>();
                }
                exceptions.add(e);
                // The attribute is kept all the same, unresolved. Discarding it would lose data the
                // document really has because of a namespace error.
                atts.addAttribute("", attQName, attQName, type, value);
            }
        }

        // Now the held-back errors, with the list already consistent.
        if (exceptions != null && errorHandler != null) {
            for (int i = 0; i < exceptions.size(); i++) {
                SAXException e = exceptions.get(i);
                errorHandler.error((SAXParseException) e);
            }
        }

        if (contentHandler != null) {
            String name[] = processName(qName, false, false);
            contentHandler.startElement(name[0], name[1], name[2], atts);
        }
    }

    // The mirror of startElement: resolve the name, emit endElement, and only then take apart the
    // prefixes this element declared; in that order, because endPrefixMapping means "the mapping
    // has ended", that is after the element that had it has closed.
    public void endElement(String qName) throws SAXException {
        if (!namespaces) {
            if (contentHandler != null) {
                contentHandler.endElement("", "", canon(qName));
            }
            return;
        }

        String names[] = processName(qName, false, false);
        if (contentHandler != null) {
            contentHandler.endElement(names[0], names[1], names[2]);
            // It is called `declared` so as not to shadow the `prefixes` feature flag further up.
            Enumeration<String> declared = nsSupport.getDeclaredPrefixes();
            while (declared.hasMoreElements()) {
                String prefix = declared.nextElement();
                contentHandler.endPrefixMapping(prefix);
            }
        }
        nsSupport.popContext();
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.characters(ch, start, length);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.ignorableWhitespace(ch, start, length);
        }
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
        if (contentHandler != null) {
            contentHandler.processingInstruction(target, data);
        }
    }

    ////////////////////////////////////////////////////////////////////

    // It wires the wrapped parser for one analysis. The document handler is always this adapter;
    // the other three are the caller's, and if they are not set they are left as they were on the
    // parser, so that a parser configured directly keeps what it had.
    private void setupParser() {
        // It goes with a guard because setFeature keeps both from being false, so getting here
        // means somebody went behind the public API.
        if (!prefixes && !namespaces) {
            throw new IllegalStateException();
        }

        nsSupport.reset();
        if (uris) {
            nsSupport.setNamespaceDeclUris(true);
        }

        if (entityResolver != null) {
            parser.setEntityResolver(entityResolver);
        }
        if (dtdHandler != null) {
            parser.setDTDHandler(dtdHandler);
        }
        if (errorHandler != null) {
            parser.setErrorHandler(errorHandler);
        }
        parser.setDocumentHandler(this);
    }

    // It resolves a qualified name. An unbound prefix is a recoverable error, and `useException`
    // says in which form the caller wants to find out: attributes want the exception so as to be
    // able to keep the attribute in degraded form, element names want the error reported and a
    // usable triple back so that the analysis goes on.
    private String[] processName(String qName, boolean isAttribute,
                                 boolean useException) throws SAXException {
        String parts[] = nsSupport.processName(qName, nameParts, isAttribute);
        if (parts == null) {
            if (useException) {
                throw makeException("Undeclared prefix: " + qName);
            }
            reportError("Undeclared prefix: " + qName);
            parts = new String[3];
            parts[0] = parts[1] = "";
            parts[2] = canon(qName);
        }
        return parts;
    }

    // It is recoverable, so it goes to error() and not to fatalError(); with no error handler it is
    // discarded, which is the SAX convention for a problem nobody asked to hear about.
    void reportError(String message) throws SAXException {
        if (errorHandler != null) {
            errorHandler.error(makeException(message));
        }
    }

    // With a position when the parser gave a locator, and explicitly with no position when it did
    // not: -1/-1 and not 0/0, so that nobody reads it as "line zero".
    private SAXParseException makeException(String message) {
        if (locator != null) {
            return new SAXParseException(message, locator);
        } else {
            return new SAXParseException(message, null, null, -1, -1);
        }
    }

    // See the long comment in NamespaceSupport. The note said that String.intern() is native and
    // the house VM does not implement it; it does now, so the probe answers true and names are
    // interned. The JDK interns every name it reports; it is not part of the SAX contract, but
    // where it can be done it is done all the same.
    private static final boolean CAN_INTERN = probeIntern();

    private static boolean probeIntern() {
        try {
            String s = "";
            return s.intern() != null;
        } catch (Throwable e) {
            return false;
        }
    }

    private static String canon(String s) {
        if (CAN_INTERN) {
            return s.intern();
        }
        return s;
    }

    private void checkNotParsing(String type, String name)
            throws SAXNotSupportedException {
        if (parsing) {
            throw new SAXNotSupportedException("Cannot change " + type + ' '
                                               + name + " while parsing");
        }
    }

    ////////////////////////////////////////////////////////////////////

    // A SAX1 AttributeList seen through the Attributes face of SAX2, used only when namespace
    // processing is off. Everything shaped like a namespace answers empty or absent, because with
    // namespaces off there is nothing to answer with: getURI and getLocalName give "", and the two
    // lookups by (uri, localName) find nothing.
    final class AttributeListAdapter implements Attributes {

        private AttributeList qAtts;

        AttributeListAdapter() {
        }

        void setAttributeList(AttributeList qAtts) {
            this.qAtts = qAtts;
        }

        public int getLength() {
            return qAtts.getLength();
        }

        public String getURI(int i) {
            return "";
        }

        public String getLocalName(int i) {
            return "";
        }

        public String getQName(int i) {
            return qAtts.getName(i);
        }

        public String getType(int i) {
            return qAtts.getType(i);
        }

        public String getValue(int i) {
            return qAtts.getValue(i);
        }

        public int getIndex(String uri, String localName) {
            return -1;
        }

        public int getIndex(String qName) {
            int max = qAtts.getLength();
            for (int i = 0; i < max; i++) {
                if (qAtts.getName(i).equals(qName)) {
                    return i;
                }
            }
            return -1;
        }

        public String getType(String uri, String localName) {
            return null;
        }

        public String getType(String qName) {
            return qAtts.getType(qName);
        }

        public String getValue(String uri, String localName) {
            return null;
        }

        public String getValue(String qName) {
            return qAtts.getValue(qName);
        }
    }
}
