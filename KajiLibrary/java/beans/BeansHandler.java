package java.beans;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// An element of the document while it is being read: its attributes, the arguments that have been
// arriving from its children, and the value it produces.
//
// The value is worked out ONCE and late: not when the element opens but when somebody asks for it.
// Whoever asks is usually a child needing its container as a target --a `<void property="x">` needs
// the bean to call `setX` on-- and that request is precisely the signal that no more arguments are
// going to reach the constructor. Hence adding an argument to an already evaluated element is an
// error and not an oversight: it means the document describes an object constructed with arguments
// the object can no longer receive.
final class Element {

    final Element parent;
    final String name;
    final Map<String, String> attributes = new HashMap<String, String>();
    final List<Object> args = new ArrayList<Object>();
    final StringBuilder text = new StringBuilder();

    boolean evaluated;
    boolean inProgress;
    Object value;

    // An element that failed to evaluate contributes no value: it is counted neither as an argument
    // of its container nor as an object of the document. It is the difference between "it is worth
    // null" and "it is worth nothing".
    boolean empty;

    Element(Element parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    // `<void>` describes an effect on the container, not a value for it; `<java>` is the root and
    // has no container. Everything else is a value that ends up among the arguments of whoever
    // contains it.
    boolean isArgument() {
        return !this.name.equals("void") && !this.name.equals("java");
    }
}

// The builder of the graph out of the events of a `java.beans` document.
//
// It is a real SAX DefaultHandler --it is what `XMLDecoder.createHandler` hands over, and the type
// that method declares it returns-- and it is also the engine XMLDecoder uses inside, fed by this
// package's own parser. That they are the same object is no thrift: it is what guarantees the
// document is read the same way wherever it comes from.
//
// The dialect it understands is that of long-term bean persistence, the one XMLEncoder writes:
//
//   `<java>`               the root. Its VALUE is the object handed to the constructor --for
//                          XMLDecoder, the decoder itself, which is what makes
//                          `<object property="owner"/>` be `decoder.getOwner()`. The values hanging
//                          off it are the document's objects.
//   `<null/>`              null.
//   `<string>`             the text, with the children's value interleaved where they appear.
//   `<class>`              a java.lang.Class, the primitives and the array descriptors included.
//   `<boolean> <byte> <char> <short> <int> <long> <float> <double>`
//                          the matching wrapper. The integers are read with `decode`, so `010` is 8
//                          and `0x1f` is 31; it is what the JDK does.
//   `<array class= length=>`  an array, with the elements its `<void index=>` put in.
//   `<object>` / `<void>`  a call. The same except in one thing: `<object>` contributes its result
//                          as an argument of whoever contains it and `<void>` does not.
//
// `<object>`/`<void>`'s attributes combine like this: `idref` cuts short and returns the variable;
// `class` sets the target (and with no `method` the call is the constructor); with no `class` the
// target is the containing element's value; `field` reads or writes a field; `property` translates
// to `getX`/`setX` according to whether there is an argument; and `index` beats `property` and
// translates to `get`/`set` with the index in front, which is what the JDK does and what writing
// arrays and lists needs.
//
// The variables (`id`) are noted down when the element OPENS, and they point at the element and not
// at its value. Without that a cyclic graph cannot be read:
// `<object id="l0"><void method="add"><object idref="l0"/>` asks for the variable before its own
// element has finished, and only by pointing at the element can it be resolved by forcing its
// evaluation at that moment.
//
// **What it does NOT do**: the objects it builds cannot be taken out of here through the public
// interface. `createHandler` declares it returns a `DefaultHandler`, which has no way of handing
// over a result, and the JDK is in the same position: its handler lives in an internal package no
// module exports. What IS observable, and what that method is there for, are the calls the document
// makes on the `owner` from `<java>`'s level.
final class BeansHandler extends org.xml.sax.helpers.DefaultHandler {

    private final Object rootValue;
    private final ClassLoader loader;
    private ExceptionListener listener;

    private final List<Object> objects = new ArrayList<Object>();
    private final Map<String, Element> variables = new HashMap<String, Element>();
    private Element current;

    BeansHandler(Object rootValue, ExceptionListener listener, ClassLoader loader) {
        this.rootValue = rootValue;
        this.listener = listener;
        this.loader = loader;
    }

    void setListener(ExceptionListener listener) {
        this.listener = listener;
    }

    ExceptionListener effectiveListener() {
        return this.listener != null ? this.listener : Delegates.DEFAULT_LISTENER;
    }

    List<Object> objects() {
        return this.objects;
    }

    // ------------------------------------------------------------------ input, without SAX

    // The three entry points the builder really uses. SAX's
    // `startElement`/`characters`/`endElement` are no more than adapters to these: that way
    // XMLDecoder's internal path does not depend on an implementation of `org.xml.sax.Attributes`
    // existing, which in this tree is an interface.
    void open(String name, Map<String, String> attributes) {
        Element e = new Element(this.current, name);
        if (attributes != null) {
            e.attributes.putAll(attributes);
        }
        this.current = e;
        String id = e.attributes.get("id");
        if (id != null) {
            this.variables.put(id, e);
        }
    }

    void text(String s) {
        if (this.current != null) {
            this.current.text.append(s);
        }
    }

    void close() {
        Element e = this.current;
        if (e == null) {
            return;
        }
        this.current = e.parent;
        this.valueOf(e);
        if (!e.empty && e.isArgument() && e.parent != null) {
            this.addArgument(e.parent, e.value);
        }
    }

    // ------------------------------------------------------------------ SAX

    public void startDocument() {
        this.objects.clear();
        this.variables.clear();
        this.current = null;
    }

    public void startElement(String uri, String localName, String qName,
            org.xml.sax.Attributes attributes) {
        Map<String, String> m = new HashMap<String, String>();
        if (attributes != null) {
            int n = attributes.getLength();
            for (int i = 0; i < n; i++) {
                String key = attributes.getQName(i);
                if (key == null || key.length() == 0) {
                    key = attributes.getLocalName(i);
                }
                m.put(key, attributes.getValue(i));
            }
        }
        this.open(nameOf(qName, localName), m);
    }

    public void characters(char[] ch, int start, int length) {
        this.text(new String(ch, start, length));
    }

    public void endElement(String uri, String localName, String qName) {
        this.close();
    }

    private static String nameOf(String qName, String localName) {
        return qName != null && qName.length() > 0 ? qName : localName;
    }

    // ------------------------------------------------------------------ evaluation

    private void addArgument(Element parent, Object v) {
        if (parent.name.equals("string")) {
            // A value inside a `<string>` is concatenated where it appears:
            // `<string>a<int>9</int>b` is "a9b". That is why the text is gathered in the same buffer
            // and in order.
            parent.text.append(v);
        } else if (parent.name.equals("java")) {
            // The root always accepts: each value hanging off it is an object of the document, and
            // its value having been asked for already --any top-level `<void>` does that-- is no
            // reason to cut the list short.
            this.objects.add(v);
        } else if (parent.evaluated || parent.inProgress) {
            this.report(new IllegalStateException("Could not add argument to evaluated element"));
        } else {
            parent.args.add(v);
        }
    }

    // The element's value, worked out once only.
    private Object valueOf(Element e) {
        if (!e.evaluated) {
            if (e.inProgress) {
                this.report(new IllegalStateException(
                    "<" + e.name + "> depends on its own value in order to work it out"));
                return null;
            }
            e.inProgress = true;
            try {
                e.value = this.compute(e);
            } catch (Exception ex) {
                this.report(ex);
                e.value = null;
                // A call that failed is not worth null: it is worth nothing. A loose value that
                // could not be read IS worth null, which is how the JDK behaves and what lets the
                // list of objects show that that place in the document was not understood.
                e.empty = isCall(e);
            } finally {
                e.inProgress = false;
                e.evaluated = true;
            }
        }
        return e.value;
    }

    private static boolean isCall(Element e) {
        return e.name.equals("object") || e.name.equals("void") || e.name.equals("array");
    }

    private Object compute(Element e) throws Exception {
        String n = e.name;
        Object r;
        if (n.equals("java")) {
            r = this.rootValue;
        } else if (n.equals("null")) {
            r = null;
        } else if (n.equals("string")) {
            r = e.text.toString();
        } else if (n.equals("class")) {
            r = classForName(e.text.toString(), this.loader);
        } else if (n.equals("object") || n.equals("void")) {
            r = this.computeCall(e);
        } else if (n.equals("array")) {
            r = this.computeArray(e);
        } else {
            r = primitive(n, e);
        }
        return r;
    }

    // The eight primitives. The integers through `decode` and not through `parseX`: it is what the
    // JDK does, and it is what gives `<char>`'s `code` attribute its meaning, which is written in
    // octal or in hex.
    private static Object primitive(String n, Element e) {
        String s = e.text.toString();
        Object r;
        if (n.equals("boolean")) {
            if (s.equalsIgnoreCase("true")) {
                r = Boolean.TRUE;
            } else if (s.equalsIgnoreCase("false")) {
                r = Boolean.FALSE;
            } else {
                throw new IllegalArgumentException("Unsupported boolean argument: " + s);
            }
        } else if (n.equals("byte")) {
            r = Byte.decode(s);
        } else if (n.equals("short")) {
            r = Short.decode(s);
        } else if (n.equals("int")) {
            r = Integer.decode(s);
        } else if (n.equals("long")) {
            r = Long.decode(s);
        } else if (n.equals("float")) {
            r = Float.valueOf(s);
        } else if (n.equals("double")) {
            r = Double.valueOf(s);
        } else if (n.equals("char")) {
            String code = e.attributes.get("code");
            if (code != null) {
                r = Character.valueOf((char) Integer.decode(code).intValue());
            } else if (s.length() == 1) {
                r = Character.valueOf(s.charAt(0));
            } else {
                throw new IllegalArgumentException("Unsupported char argument: " + s);
            }
        } else {
            throw new IllegalArgumentException("Unsupported element: <" + n + ">");
        }
        return r;
    }

    // `class` is the COMPONENT type, not the array's. With no `length` the length is given by the
    // loose values hanging off it; with `length` the array is born that size and the `<void index=>`
    // fill it. If both came, both are used, which is reading the document the most obvious way; the
    // JDK in that case rejects the loose values.
    private Object computeArray(Element e) throws Exception {
        String className = e.attributes.get("class");
        if (className == null) {
            throw new IllegalArgumentException("<array> sin attribute class");
        }
        Class<?> component = classForName(className, this.loader);
        String length = e.attributes.get("length");
        int n = length != null ? Integer.parseInt(length) : e.args.size();
        Object array = Array.newInstance(component, n);
        for (int i = 0; i < e.args.size() && i < n; i++) {
            Statement.putInArray(array, i, e.args.get(i));
        }
        return array;
    }

    private Object computeCall(Element e) throws Exception {
        String idref = e.attributes.get("idref");
        if (idref != null) {
            return this.variable(idref);
        }

        Class<?> type = null;
        String className = e.attributes.get("class");
        if (className != null) {
            type = classForName(className, this.loader);
        }
        Object[] args = e.args.toArray(new Object[e.args.size()]);

        String fieldName = e.attributes.get("field");
        if (fieldName != null) {
            Object base = type != null ? null : this.context(e);
            Class<?> owner = type != null ? type : base.getClass();
            Field f = owner.getField(fieldName);
            Object r = null;
            if (args.length == 0) {
                r = f.get(base);
            } else {
                f.set(base, args[0]);
            }
            return r;
        }

        Object target = type != null ? type : this.context(e);
        String method = e.attributes.get("method");
        String property = e.attributes.get("property");
        if (property != null) {
            method = (args.length == 0 ? "get" : "set") + PropertyDescriptor.capitalize(property);
        }
        String index = e.attributes.get("index");
        if (index != null) {
            // The index beats the property, just as in the JDK: `index` describes positional
            // access --an array's or a list's-- and that access is called plain `get`/`set`.
            method = args.length == 0 ? "get" : "set";
            Object[] withIndex = new Object[args.length + 1];
            withIndex[0] = Integer.valueOf(index);
            System.arraycopy(args, 0, withIndex, 1, args.length);
            args = withIndex;
        }
        if (method == null) {
            method = "new";
        }
        return new Expression(target, method, args).getValue();
    }

    private Object context(Element e) {
        if (e.parent == null) {
            throw new IllegalStateException("<" + e.name + "> fuera de whole elemento container");
        }
        Object v = this.valueOf(e.parent);
        if (v == null) {
            throw new IllegalStateException("Context bean is not created");
        }
        return v;
    }

    private Object variable(String name) {
        Element e = this.variables.get(name);
        if (e == null) {
            throw new IllegalArgumentException("Unbound variable: " + name);
        }
        return this.valueOf(e);
    }

    // The nine names that belong to no loadable class, and then the loader.
    static Class<?> classForName(String name, ClassLoader loader) throws ClassNotFoundException {
        Class<?> r;
        if (name.equals("boolean")) { r = boolean.class; }
        else if (name.equals("byte")) { r = byte.class; }
        else if (name.equals("char")) { r = char.class; }
        else if (name.equals("short")) { r = short.class; }
        else if (name.equals("int")) { r = int.class; }
        else if (name.equals("long")) { r = long.class; }
        else if (name.equals("float")) { r = float.class; }
        else if (name.equals("double")) { r = double.class; }
        else if (name.equals("void")) { r = void.class; }
        else if (loader != null) { r = Class.forName(name, true, loader); }
        else { r = Class.forName(name); }
        return r;
    }

    void report(Exception e) {
        this.effectiveListener().exceptionThrown(e);
    }
}
