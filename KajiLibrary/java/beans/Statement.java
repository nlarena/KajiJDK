package java.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// A call stored to be executed later: a target, a method name and its arguments. It is the unit
// persistence describes "how this object is remade" with -- a graph of objects is stored as the
// sequence of calls that rebuilds it, not as its bytes.
//
// The name "new" is special and means constructor: `new Statement(Foo.class, "new", args)` runs
// `new Foo(args)`.
//
// The method resolution cannot go by exact descriptor: the arguments arrive as Object, so an `int`
// comes wrapped in an Integer and has to be accepted where `int` was declared. That is why the
// candidates are walked and the first one whose parameters ACCEPT the given arguments is chosen.
public class Statement {

    private Object target;
    private String methodName;
    private Object[] arguments;

    public Statement(Object target, String methodName, Object[] arguments) {
        this.target = target;
        this.methodName = methodName;
        this.arguments = arguments == null ? new Object[0] : arguments;
    }

    public Object getTarget() {
        return this.target;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public Object[] getArguments() {
        return this.arguments;
    }

    // It runs the call and discards the result. Expression overrides it to keep it.
    public void execute() throws Exception {
        this.emitCall();
    }

    // The engine shared with Expression.
    Object emitCall() throws Exception {
        if (this.target == null) {
            throw new NullPointerException("target should not be null");
        }
        if (this.methodName == null) {
            throw new NullPointerException("method name should not be null");
        }

        Object result;
        if (this.target.getClass().isArray()
                && ("get".equals(this.methodName) || "set".equals(this.methodName))) {
            // Arrays have no methods: `get`/`set` on an array are indexed access. It is the same
            // special case the JDK makes, and it is the one that lets persistence describe "element
            // 3 of this array" as one more call.
            int index = ((Integer) this.arguments[0]).intValue();
            if ("get".equals(this.methodName)) {
                result = arrayElement(this.target, index);
            } else {
                putInArray(this.target, index, this.arguments[1]);
                result = null;
            }
        } else if ("new".equals(this.methodName)) {
            if (!(this.target instanceof Class)) {
                throw new NoSuchMethodException("\"new\" needs a Class target");
            }
            result = this.construct((Class<?>) this.target);
        } else if (this.target instanceof Class) {
            // A Class target may mean either "call a static of that class" or "call a method of
            // the Class instance". The static is tried first, which is what whoever wrote the
            // Statement meant.
            Method m = this.findFor((Class<?>) this.target, true);
            if (m != null) {
                result = m.invoke(null, this.arguments);
            } else {
                Method mc = this.findFor(this.target.getClass(), false);
                if (mc == null) {
                    throw new NoSuchMethodException(this.description());
                }
                result = mc.invoke(this.target, this.arguments);
            }
        } else {
            Method m = this.findFor(this.target.getClass(), false);
            if (m == null) {
                throw new NoSuchMethodException(this.description());
            }
            result = m.invoke(this.target, this.arguments);
        }
        return result;
    }

    private Object construct(Class<?> c) throws Exception {
        // Character is the only wrapper with no constructor from a String. Persistence describes
        // every wrapper the same way --`new Integer("7")`, `new Boolean("true")`-- so instead of
        // giving Character a delegate of its own, that non-existent constructor is faked here. It is
        // the same patch the JDK makes and in the same place: without it, every bean with a `char`
        // property would fail on rereading its own value.
        if (c == Character.class && this.arguments.length == 1
                && this.arguments[0] instanceof String) {
            String s = (String) this.arguments[0];
            if (s.length() == 0) {
                throw new IllegalArgumentException("empty string for Character");
            }
            return Character.valueOf(s.charAt(0));
        }
        Constructor<?>[] cs = c.getConstructors();
        Constructor<?> chosen = null;
        for (int i = 0; i < cs.length; i++) {
            if (chosen == null && accept(cs[i].getParameterTypes(), this.arguments)) {
                chosen = cs[i];
            }
        }
        if (chosen == null) {
            throw new NoSuchMethodException(this.description());
        }
        return chosen.newInstance(this.arguments);
    }

    // The first public method with that name whose parameters accept the arguments.
    private Method findFor(Class<?> c, boolean staticsOnly) {
        Method chosen = null;
        Method[] ms = c.getMethods();
        for (int i = 0; i < ms.length; i++) {
            Method m = ms[i];
            if (chosen == null
                    && m.getName().equals(this.methodName)
                    && Modifier.isStatic(m.getModifiers()) == staticsOnly
                    && accept(m.getParameterTypes(), this.arguments)) {
                chosen = m;
            }
        }
        return chosen;
    }

    private String description() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.methodName).append('(');
        for (int i = 0; i < this.arguments.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.arguments[i] == null ? "null" : this.arguments[i].getClass().getName());
        }
        sb.append(')');
        return sb.toString();
    }

    // Whether those declared parameters admit those arguments.
    static boolean accept(Class<?>[] params, Object[] args) {
        boolean ok = params.length == args.length;
        for (int i = 0; ok && i < params.length; i++) {
            ok = accepts(params[i], args[i]);
        }
        return ok;
    }

    // A primitive parameter accepts its wrapper and nothing else --not even null; a reference
    // parameter accepts null and any instance of itself.
    static boolean accepts(Class<?> param, Object arg) {
        boolean ok;
        if (param.isPrimitive()) {
            ok = arg != null && wrapperOf(param) == arg.getClass();
        } else if (arg == null) {
            ok = true;
        } else {
            ok = param.isAssignableFrom(arg.getClass());
        }
        return ok;
    }

    // The three array accesses go by type dispatch and not through `java.lang.reflect.Array`: in
    // this VM `Array.get`, `Array.set` and `Array.getLength` are `native` with no registered
    // implementation and throw UnsatisfiedLinkError. With the explicit dispatch, array persistence
    // works all the same and does not depend on natives that are not there.
    static int arrayLength(Object a) {
        int n;
        if (a instanceof Object[]) { n = ((Object[]) a).length; }
        else if (a instanceof int[]) { n = ((int[]) a).length; }
        else if (a instanceof boolean[]) { n = ((boolean[]) a).length; }
        else if (a instanceof byte[]) { n = ((byte[]) a).length; }
        else if (a instanceof char[]) { n = ((char[]) a).length; }
        else if (a instanceof short[]) { n = ((short[]) a).length; }
        else if (a instanceof long[]) { n = ((long[]) a).length; }
        else if (a instanceof float[]) { n = ((float[]) a).length; }
        else if (a instanceof double[]) { n = ((double[]) a).length; }
        else { throw new IllegalArgumentException("Argument is not an array"); }
        return n;
    }

    static Object arrayElement(Object a, int i) {
        Object v;
        if (a instanceof Object[]) { v = ((Object[]) a)[i]; }
        else if (a instanceof int[]) { v = Integer.valueOf(((int[]) a)[i]); }
        else if (a instanceof boolean[]) { v = Boolean.valueOf(((boolean[]) a)[i]); }
        else if (a instanceof byte[]) { v = Byte.valueOf(((byte[]) a)[i]); }
        else if (a instanceof char[]) { v = Character.valueOf(((char[]) a)[i]); }
        else if (a instanceof short[]) { v = Short.valueOf(((short[]) a)[i]); }
        else if (a instanceof long[]) { v = Long.valueOf(((long[]) a)[i]); }
        else if (a instanceof float[]) { v = Float.valueOf(((float[]) a)[i]); }
        else if (a instanceof double[]) { v = Double.valueOf(((double[]) a)[i]); }
        else { throw new IllegalArgumentException("Argument is not an array"); }
        return v;
    }

    static void putInArray(Object a, int i, Object v) {
        if (a instanceof Object[]) { ((Object[]) a)[i] = v; }
        else if (a instanceof int[]) { ((int[]) a)[i] = ((Number) v).intValue(); }
        else if (a instanceof boolean[]) { ((boolean[]) a)[i] = ((Boolean) v).booleanValue(); }
        else if (a instanceof byte[]) { ((byte[]) a)[i] = ((Number) v).byteValue(); }
        else if (a instanceof char[]) { ((char[]) a)[i] = ((Character) v).charValue(); }
        else if (a instanceof short[]) { ((short[]) a)[i] = ((Number) v).shortValue(); }
        else if (a instanceof long[]) { ((long[]) a)[i] = ((Number) v).longValue(); }
        else if (a instanceof float[]) { ((float[]) a)[i] = ((Number) v).floatValue(); }
        else if (a instanceof double[]) { ((double[]) a)[i] = ((Number) v).doubleValue(); }
        else { throw new IllegalArgumentException("Argument is not an array"); }
    }

    // wrapperOf's inverse path: null if the class is not a wrapper.
    static Class<?> primitiveOfWrapper(Class<?> wrapper) {
        Class<?> r = null;
        if (wrapper == Integer.class) { r = int.class; }
        else if (wrapper == Boolean.class) { r = boolean.class; }
        else if (wrapper == Long.class) { r = long.class; }
        else if (wrapper == Double.class) { r = double.class; }
        else if (wrapper == Float.class) { r = float.class; }
        else if (wrapper == Short.class) { r = short.class; }
        else if (wrapper == Byte.class) { r = byte.class; }
        else if (wrapper == Character.class) { r = char.class; }
        return r;
    }

    static Class<?> wrapperOf(Class<?> primitive) {
        Class<?> r = null;
        if (primitive == int.class) { r = Integer.class; }
        else if (primitive == boolean.class) { r = Boolean.class; }
        else if (primitive == long.class) { r = Long.class; }
        else if (primitive == double.class) { r = Double.class; }
        else if (primitive == float.class) { r = Float.class; }
        else if (primitive == short.class) { r = Short.class; }
        else if (primitive == byte.class) { r = Byte.class; }
        else if (primitive == char.class) { r = Character.class; }
        return r;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.target == null ? "null" : EventSetDescriptor.simpleName(this.target.getClass()));
        sb.append('.').append(this.methodName).append('(');
        for (int i = 0; i < this.arguments.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.arguments[i]);
        }
        sb.append(");");
        return sb.toString();
    }
}
