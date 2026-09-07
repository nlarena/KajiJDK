package java.beans;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// The Encoder that also prints. All the intelligence --which calls remake the graph-- was already
// put there by Encoder; what this class adds is the textual representation of those calls and, above
// all, **the aliasing**: deciding which object needs an `id` because several things point at it, and
// which one can be written inline.
//
// That is why it cannot print as it encodes. While the graph is being walked it is not yet known
// whether an object is going to appear once or five times, and that answer changes how it is
// written: inline, or as `<object id="foo0">` plus `<object idref="foo0"/>` in the other places. So
// the encoding **accumulates** --each call is stored in its target's list-- and `flush()` is the one
// that walks what was accumulated and only then prints. Hence `close()` being compulsory: without a
// flush nothing comes out.
//
// The reference counting is the only delicate part. `mark` walks the graph once and adds one
// reference for each place the object appears as an argument; `printCall` gives an `id` only to
// those that gathered more than one. A shared object coming out twice inline would be a different
// graph from the original --two objects where there was one-- and that is exactly the mistake the
// `id` avoids.
//
// ## What it does NOT do
//
// **The `charset` can only be UTF-8.** It is no gratuitous simplification: in this tree
// `java.io.OutputStreamWriter` ignores the charset handed to it and truncates each char to its low
// byte (checked: "ñ€" comes out as `f1 ac` instead of `c3 b1 e2 82 ac`), so leaning on it would give
// a file that says `encoding="UTF-8"` and is not. Here the UTF-8 encoding is done by hand over the
// OutputStream --correctly, surrogate pairs included-- and any other charset name is rejected in the
// constructor with IllegalArgumentException. The JDK throws UnsupportedCharsetException in that very
// place, which is ALSO an IllegalArgumentException, so whoever catches the error case behaves the
// same; what changes is the set of accepted charsets, and that is a declared limitation and not a
// lie about the file's contents.
public class XMLEncoder extends Encoder implements AutoCloseable {

    private static final String SALTO = lineBreak();

    private final OutputStream out2;
    private final String charset;
    private final boolean declaracion;

    private Object owner;
    private int indent;
    private boolean preamble;

    // While it is true, the calls arriving come from Encoder's machinery and not from a user's
    // `writeObject`. The distinction matters: a writeObject from outside is a root of the document,
    // one from inside is a part of something already being written.
    private boolean interno;

    private final Map<Object, Info> datos = new IdentityHashMap<Object, Info>();
    private final Map<Object, List<Statement>> byTarget = new IdentityHashMap<Object, List<Statement>>();

    // The same lists as `byTarget`'s, in an index that can be walked without touching the keys. See
    // `looseCall`.
    private final List<List<Statement>> allLists = new ArrayList<List<Statement>>();

    // The name generator for the `id`s: "Vector0", "Vector1", ... It is by identity because two
    // equal but distinct objects have to carry different names.
    private final Map<Object, String> idNames = new IdentityHashMap<Object, String>();
    private final Map<String, Integer> counters = new HashMap<String, Integer>();

    // What is known about an object of the graph while it is being accumulated.
    private static final class Info {
        Expression exp;     // the expression that produces it
        int refs;           // how many places point at it
        boolean marked;     // `mark` has walked it already
        String idName;      // the `id` it was given, if it needed one
    }

    public XMLEncoder(OutputStream out) {
        this(out, "UTF-8", true, 0);
    }

    public XMLEncoder(OutputStream out, String charset, boolean declaration, int indentation) {
        if (out == null) {
            throw new IllegalArgumentException("the output stream cannot be null");
        }
        if (indentation < 0) {
            throw new IllegalArgumentException("the indentation must be >= 0");
        }
        if (charset == null) {
            throw new IllegalArgumentException("the charset cannot be null");
        }
        if (!isUtf8(charset)) {
            throw new IllegalArgumentException("unsupported charset: " + charset
                + " (this implementation only encodes UTF-8)");
        }
        this.out2 = out;
        this.charset = charset;
        this.declaracion = declaration;
        this.indent = indentation;
    }

    private static boolean isUtf8(String idName) {
        String n = idName.toUpperCase();
        return n.equals("UTF-8") || n.equals("UTF8") || n.equals("UNICODE-1-1-UTF-8");
    }

    private static String lineBreak() {
        String s = null;
        try {
            s = System.getProperty("line.separator");
        } catch (Exception e) {
            s = null;
        }
        return s == null ? "\n" : s;
    }

    public Object getOwner() {
        return this.owner;
    }

    // Besides storing it, it is noted down as the value of the expression `getOwner()` on this
    // encoder. That way, when the graph points at the owner, instead of describing it whole again
    // out comes `<object property="owner">` -- which is what the decoder needs in order to plug ITS
    // own owner back in when reading.
    public void setOwner(Object owner) {
        this.owner = owner;
        this.writeExpression(new Expression(owner, this, "getOwner", new Object[0]));
    }

    // A root of the document. It is represented as the call `this.writeObject(o)`, and flush()
    // recognizes it by name so as to print the value and not the call.
    public void writeObject(Object o) {
        if (this.interno) {
            super.writeObject(o);
        } else {
            this.writeStatement(new Statement(this, "writeObject", new Object[] { o }));
        }
    }

    public void writeStatement(Statement oldStm) {
        boolean previous = this.interno;
        this.interno = true;
        try {
            super.writeStatement(oldStm);
            // The marking goes BEFORE queueing: the call may depend on values established in
            // earlier calls of this same context.
            this.mark(oldStm);
            Object target = oldStm.getTarget();
            if (target instanceof Field) {
                // A `field.get(x)` / `field.set(x, v)` describes `x`'s state, not the Field's: the
                // call has to end up hanging off x or it would come out outside the object it
                // belongs to.
                String methodName = oldStm.getMethodName();
                Object[] args = oldStm.getArguments();
                if (methodName != null && args != null) {
                    if (methodName.equals("get") && args.length == 1) {
                        target = args[0];
                    } else if (methodName.equals("set") && args.length == 2) {
                        target = args[0];
                    }
                }
            }
            this.listOf(target).add(oldStm);
        } catch (Exception e) {
            this.getExceptionListener().exceptionThrown(
                new Exception("XMLEncoder: discarding statement " + oldStm, e));
        }
        this.interno = previous;
    }

    public void writeExpression(Expression oldExp) {
        boolean previous = this.interno;
        this.interno = true;
        Object value = this.valueOf(oldExp);
        // The condition on the string is on purpose: a string arriving from outside --not from the
        // machinery-- is written even if it already has a link, because the user asked for it as a
        // root.
        if (this.get(value) == null || (value instanceof String && !previous)) {
            this.dataOf(value).exp = oldExp;
            super.writeExpression(oldExp);
        }
        this.interno = previous;
    }

    // It prints everything accumulated and empties the state. The preamble comes out with the first
    // flush and not in the constructor: writing in the constructor would make an XMLEncoder that is
    // never used leave a file with an unclosed `<java>`.
    public void flush() {
        if (!this.preamble) {
            if (this.declaracion) {
                this.line("<?xml version=" + quoted("1.0")
                    + " encoding=" + quoted(this.charset) + "?>");
            }
            this.line("<java version=" + quoted(javaVersion())
                + " class=" + quoted("java.beans.XMLDecoder") + ">");
            this.preamble = true;
        }

        this.indent++;
        List<Statement> roots = this.listOf(this);
        while (!roots.isEmpty()) {
            Statement s = roots.remove(0);
            if ("writeObject".equals(s.getMethodName())) {
                this.printValue(s.getArguments()[0], this, true);
            } else {
                this.printCall(s, this, false);
            }
        }
        this.indent--;

        // Calls left hanging off a target that was never printed. Losing them would be losing state
        // of the graph silently, so they are emitted all the same at the level above.
        Statement suelta = this.looseCall();
        while (suelta != null) {
            this.printCall(suelta, this, false);
            suelta = this.looseCall();
        }

        try {
            this.out2.flush();
        } catch (IOException e) {
            this.getExceptionListener().exceptionThrown(e);
        }
        this.limpiar();
    }

    public void close() {
        this.flush();
        this.line("</java>");
        try {
            this.out2.close();
        } catch (IOException e) {
            this.getExceptionListener().exceptionThrown(e);
        }
    }

    private static String javaVersion() {
        String v = null;
        try {
            v = System.getProperty("java.version");
        } catch (Exception e) {
            v = null;
        }
        return v == null ? "" : v;
    }

    private void limpiar() {
        this.clearLinks();
        this.datos.clear();
        this.byTarget.clear();
        this.allLists.clear();
        this.idNames.clear();
        this.counters.clear();
    }

    // It walks the lists through the parallel index and not through `byTarget.values()`. A concrete
    // reason: in this tree `IdentityHashMap.values()` walks by hashing the KEYS, so a key whose
    // `hashCode()` does not terminate --a collection containing itself, which is exactly the case
    // this tests-- overflows the stack, even though `put` and `get` on that same key work fine
    // because they go by identity. The index keeps the same lists and touches no key.
    private Statement looseCall() {
        Statement r = null;
        Iterator<List<Statement>> it = this.allLists.iterator();
        while (r == null && it.hasNext()) {
            List<Statement> l = it.next();
            for (int i = 0; r == null && i < l.size(); i++) {
                // Pure Statements only: a loose Expression describes no state, it describes a value
                // nobody ended up using.
                if (Statement.class == l.get(i).getClass()) {
                    r = l.remove(i);
                }
            }
        }
        return r;
    }

    // The two tables are indexed by objects of the graph, and `null` is a legitimate key: a call
    // with no value --a `void`-- has the value null, and its data and its body are stored like anyone
    // else's. In this tree neither HashMap nor IdentityHashMap accepts a null key (they throw
    // NullPointerException where the JDK stores the entry), so null travels as this sentinel. It is a
    // private, unique object: it can never collide with an object of the graph.
    private static final Object NULL_KEY = new Object();

    private static Object key(Object o) {
        return o == null ? NULL_KEY : o;
    }

    private Info dataOf(Object o) {
        Object k = key(o);
        Info d = this.datos.get(k);
        if (d == null) {
            d = new Info();
            this.datos.put(k, d);
        }
        return d;
    }

    private List<Statement> listOf(Object target) {
        Object k = key(target);
        List<Statement> l = this.byTarget.get(k);
        if (l == null) {
            l = new ArrayList<Statement>();
            this.byTarget.put(k, l);
            this.allLists.add(l);
        }
        return l;
    }

    // It walks the graph from a call adding up references. Each object is walked once only --the
    // `marked` flag-- but its reference count goes up every time it appears as an argument: that is
    // exactly "how many places point at it", which is what decides whether it needs an `id`.
    private void mark(Statement stm) {
        Object[] args = stm.getArguments();
        for (int i = 0; i < args.length; i++) {
            this.mark(args[i], true);
        }
        this.mark(stm.getTarget(), stm instanceof Expression);
    }

    private void mark(Object o, boolean isArgument) {
        if (o == null || o == this) {
            return;
        }
        Info d = this.dataOf(o);
        Expression exp = d.exp;
        // A literal string --with no expression producing it-- is always written inline: it has no
        // identity worth preserving. A string that DID come from an expression (from a resource
        // bundle, say) is marked like any object.
        if (o.getClass() == String.class && exp == null) {
            return;
        }
        if (isArgument) {
            d.refs++;
        }
        if (d.marked || exp == null) {
            return;
        }
        d.marked = true;
        Object target = exp.getTarget();
        this.mark(exp);
        if (!(target instanceof Class)) {
            this.listOf(target).add(exp);
            d.refs++;
        }
    }

    private void printValue(Object value, Object container, boolean isArgument) {
        if (value == null) {
            this.line("<null/>");
            return;
        }
        if (value instanceof Class) {
            this.line("<class>" + ((Class<?>) value).getName() + "</class>");
            return;
        }

        Info d = this.dataOf(value);
        Expression exp = d.exp;

        // A wrapper remade with `new Integer("7")` is printed as `<int>7</int>`: it is the short
        // form of the same fact, and it is the one that makes the file readable.
        if (exp != null) {
            Class<?> primitivo = Statement.primitiveOfWrapper(value.getClass());
            if (primitivo != null && exp.getTarget() == value.getClass()
                    && "new".equals(exp.getMethodName())) {
                String tag = primitivo.getName();
                String text = primitivo == char.class
                    ? escape(String.valueOf(((Character) value).charValue()))
                    : String.valueOf(value);
                this.line("<" + tag + ">" + text + "</" + tag + ">");
                return;
            }
        }

        if (value instanceof String && exp == null) {
            this.line("<string>" + escape((String) value) + "</string>");
            return;
        }

        // It was printed before and carries a name: here goes the reference, not a second copy.
        if (d.idName != null) {
            this.line("<object idref=" + quoted(d.idName) + "/>");
            return;
        }

        if (exp == null) {
            // Nobody knew how to remake it. That is said, instead of writing an empty `<object>`
            // that on being read would give another object.
            this.getExceptionListener().exceptionThrown(
                new Exception("XMLEncoder: no expression for " + value.getClass().getName()));
            this.line("<null/>");
            return;
        }

        this.printCall(exp, container, isArgument);
    }

    private void printCall(Statement exp, Object container, boolean isArgument) {
        Object target = exp.getTarget();
        String methodName = exp.getMethodName();
        Object[] args = exp.getArguments();
        boolean isExpression = exp.getClass() == Expression.class;
        Object value = null;
        if (isExpression) {
            value = this.valueOf((Expression) exp);
        }

        String tag = (isExpression && isArgument) ? "object" : "void";
        StringBuilder attrs = new StringBuilder();
        Info d = this.dataOf(value);

        // A call on the object we are already writing carries no target attribute: it is
        // understood. It is also the owner's case, whose target is this very encoder.
        if (target == container) {
            attrs.setLength(0);
        } else if (target == Array.class && "newInstance".equals(methodName)) {
            tag = "array";
            attrs.append(" class=").append(quoted(((Class<?>) args[0]).getName()));
            attrs.append(" length=").append(quoted(String.valueOf(args[1])));
            args = new Object[0];
        } else if (target != null && target.getClass() == Class.class) {
            attrs.append(" class=").append(quoted(((Class<?>) target).getName()));
        } else {
            // The target is another object of the graph. IT has to be written, and this call moves
            // to hang off it. The `refs = 2` forces it to carry an `id`: if something is called on an
            // object, that object has identity and cannot be written inline twice.
            d.refs = 2;
            if (d.idName == null) {
                Info dTarget = this.dataOf(target);
                dTarget.refs = dTarget.refs + 1;
                List<Statement> l = this.listOf(target);
                if (!l.contains(exp)) {
                    l.add(exp);
                }
                this.printValue(target, container, false);
            }
            if (isExpression) {
                this.printValue(value, container, isArgument);
            }
            return;
        }

        if (isExpression && d.refs > 1) {
            String idName = this.nameOf(value);
            d.idName = idName;
            attrs.append(" id=").append(quoted(idName));
        }

        // Indexed access: `set(i, v)` and `get(i)` are written with the `index` attribute, not with
        // the index as the first argument. It is what makes an array readable.
        if ((!isExpression && "set".equals(methodName) && args.length == 2 && args[0] instanceof Integer)
                || (isExpression && "get".equals(methodName) && args.length == 1 && args[0] instanceof Integer)) {
            attrs.append(" index=").append(quoted(String.valueOf(args[0])));
            args = args.length == 1 ? new Object[0] : new Object[] { args[1] };
        } else if ((!isExpression && methodName.startsWith("set") && args.length == 1)
                || (isExpression && methodName.startsWith("get") && args.length == 0)) {
            // A get/set pair is written as the property it is. `setName(x)` -> property="name".
            if (methodName.length() > 3) {
                attrs.append(" property=").append(quoted(Introspector.decapitalize(methodName.substring(3))));
            }
        } else if (!"new".equals(methodName) && !"newInstance".equals(methodName)) {
            attrs.append(" method=").append(quoted(methodName));
        }

        List<Statement> body = this.listOf(value);

        if (args.length == 0 && body.size() == 0) {
            this.line("<" + tag + attrs + "/>");
            return;
        }

        this.line("<" + tag + attrs + ">");
        this.indent++;
        for (int i = 0; i < args.length; i++) {
            this.printValue(args[i], null, true);
        }
        while (!body.isEmpty()) {
            this.printCall(body.remove(0), value, false);
        }
        this.indent--;
        this.line("</" + tag + ">");
    }

    // "java.util.Vector" -> "Vector0", "Vector1", ... The arrays carry the "Array" suffix.
    private String nameOf(Object o) {
        String r;
        if (o == null) {
            r = "null";
        } else if (o instanceof Class) {
            r = shortName((Class<?>) o);
        } else {
            r = this.idNames.get(o);
            if (r == null) {
                String base = shortName(o.getClass());
                Integer previous = this.counters.get(base);
                int n = previous == null ? 0 : previous.intValue() + 1;
                this.counters.put(base, Integer.valueOf(n));
                r = base + n;
                this.idNames.put(o, r);
            }
        }
        return r;
    }

    private static String shortName(Class<?> kind) {
        String r;
        if (kind.isArray()) {
            r = shortName(kind.getComponentType()) + "Array";
        } else {
            String n = kind.getName();
            r = n.substring(n.lastIndexOf('.') + 1);
        }
        return r;
    }

    private static String quoted(String s) {
        return "\"" + s + "\"";
    }

    // The six characters that cannot go raw in XML. The `\r` goes as a numeric reference because a
    // parser normalizes the line endings and would eat it.
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') { sb.append("&amp;"); }
            else if (c == '<') { sb.append("&lt;"); }
            else if (c == '>') { sb.append("&gt;"); }
            else if (c == '"') { sb.append("&quot;"); }
            else if (c == '\'') { sb.append("&apos;"); }
            else if (c == '\r') { sb.append("&#13;"); }
            else { sb.append(c); }
        }
        return sb.toString();
    }

    private void line(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.indent; i++) {
            sb.append(' ');
        }
        sb.append(text).append(SALTO);
        this.emitir(sb.toString());
    }

    // UTF-8 by hand. See the header: OutputStreamWriter does not encode in this tree.
    private void emitir(String s) {
        try {
            int n = s.length();
            int i = 0;
            while (i < n) {
                int cp = s.charAt(i);
                i++;
                if (cp >= 0xD800 && cp <= 0xDBFF && i < n) {
                    char bajo = s.charAt(i);
                    if (bajo >= 0xDC00 && bajo <= 0xDFFF) {
                        cp = 0x10000 + ((cp - 0xD800) << 10) + (bajo - 0xDC00);
                        i++;
                    }
                }
                if (cp < 0x80) {
                    this.out2.write(cp);
                } else if (cp < 0x800) {
                    this.out2.write(0xC0 | (cp >> 6));
                    this.out2.write(0x80 | (cp & 0x3F));
                } else if (cp < 0x10000) {
                    this.out2.write(0xE0 | (cp >> 12));
                    this.out2.write(0x80 | ((cp >> 6) & 0x3F));
                    this.out2.write(0x80 | (cp & 0x3F));
                } else {
                    this.out2.write(0xF0 | (cp >> 18));
                    this.out2.write(0x80 | ((cp >> 12) & 0x3F));
                    this.out2.write(0x80 | ((cp >> 6) & 0x3F));
                    this.out2.write(0x80 | (cp & 0x3F));
                }
            }
        } catch (IOException e) {
            this.getExceptionListener().exceptionThrown(e);
        }
    }
}
