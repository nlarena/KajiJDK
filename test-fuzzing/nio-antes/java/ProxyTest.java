import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Behaviour test of java.lang.reflect.Proxy, written to run **the same** in this VM and in the real
 * JDK.
 *
 * <p>Each check has an index. {@code run()} returns -1 if they all passed, or the index of the
 * first one that failed: a single int is enough to compare the two VMs without depending on the
 * console output matching character by character.
 *
 * <p>No check looks at the name or the package of the generated class: the JDK changed them twice
 * (com.sun.proxy, then jdk.proxy1) and they are not part of the contract.
 */
public class ProxyTest {

    // ---- test interfaces ----

    public interface Greeting {
        String hello();
    }

    public interface Prims {
        int ints(int a, long b, double c, boolean d);
        char morePrims(char a, byte b, short c, float d);
        void nothing();
        long len();
        double doubleVal();
        float floatVal();
        boolean bool();
        byte octet();
        short shortVal();
        char character();
    }

    /**
     * It declares `name()` first: the Method that reaches the handler has to be the one from here.
     */
    public interface FirstA {
        String name();
    }

    public interface SecondB {
        String name();
    }

    /** Covariant returns: `Object` and `String` are compatible, both methods are generated. */
    public interface CovarObj {
        Object value();
    }

    public interface CovarStr {
        String value();
    }

    /** Incompatible returns: neither is String an Integer nor the other way round. */
    public interface BadA {
        String clash();
    }

    public interface BadB {
        Integer clash();
    }

    public interface Checked {
        void declared() throws java.io.IOException;
        void undeclared();
    }

    public interface WithArrays {
        int[] duplicate(int[] entry);
        String[][] matrix(String[] row);
    }

    public interface WithDefault {
        String base();
        default String derived() {
            return "default:" + this.base();
        }
    }

    /**
     * An interface that inherits its method: the Method that arrives is that of whoever DECLARES
     * it.
     */
    public interface Base {
        String inherited();
    }

    public interface Derived extends Base {
        String own();
    }

    /** Generic: the compiler puts a bridge `cmp(Object)` next to `cmp(Integer)`. */
    public interface Comparer<T> {
        int cmp(T x);
    }

    public interface CompInt extends Comparer<Integer> {
    }

    /**
     * It redeclares `toString()`: the same complete signature as Object's, it is not duplicated.
     */
    public interface WithToString {
        String toString();
    }

    /** Not public: the generated class has to land in this same package and not be public. */
    interface NotPublic {
        String secret();
    }

    // ---- handlers ----

    /** It keeps the last thing it received and returns what it was programmed with. */
    static final class Spy implements InvocationHandler {
        Object answer;
        Throwable toThrow;
        Object lastProxy;
        Method lastMethod;
        Object[] lastArgs;

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            this.lastProxy = proxy;
            this.lastMethod = method;
            this.lastArgs = args;
            if (this.toThrow != null) {
                Throwable t = this.toThrow;
                this.toThrow = null;
                throw t;
            }
            return this.answer;
        }
    }

    private static Object newProxy(Spy e, Class<?>[] ifaces) {
        return Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), ifaces, e);
    }

    public static int run() {
        Spy e = new Spy();

        // 0: the call reaches the handler and its return comes out through the method.
        e.answer = "che";
        Object p = newProxy(e, new Class<?>[] { Greeting.class });
        if (!"che".equals(((Greeting) p).hello())) {
            return 0;
        }

        // 1: the proxy IS the interface.
        if (!(p instanceof Greeting)) {
            return 1;
        }

        // 2 and 3: the class is recognised as a proxy and returns its handler.
        if (!Proxy.isProxyClass(p.getClass())) {
            return 2;
        }
        if (Proxy.getInvocationHandler(p) != e) {
            return 3;
        }

        // 4: just any class is not a proxy.
        if (Proxy.isProxyClass(String.class)) {
            return 4;
        }

        // 5: a method with no parameters receives `null`, not an empty array.
        if (e.lastArgs != null) {
            return 5;
        }

        // 6: the proxy that reaches the handler is the same object.
        if (e.lastProxy != p) {
            return 6;
        }

        // 7: the Method is that of the interface that declares it.
        if (e.lastMethod.getDeclaringClass() != Greeting.class) {
            return 7;
        }
        if (!"hola".equals(e.lastMethod.getName())) {
            return 8;
        }

        // 9..14: the primitives are boxed on the way in and unboxed on the way out.
        Spy ep = new Spy();
        Prims pr = (Prims) newProxy(ep, new Class<?>[] { Prims.class });
        ep.answer = Integer.valueOf(42);
        if (pr.ints(1, 2L, 3.5d, true) != 42) {
            return 9;
        }
        Object[] a = ep.lastArgs;
        if (a == null || a.length != 4) {
            return 10;
        }
        if (!(a[0] instanceof Integer) || ((Integer) a[0]).intValue() != 1) {
            return 11;
        }
        if (!(a[1] instanceof Long) || ((Long) a[1]).longValue() != 2L) {
            return 12;
        }
        if (!(a[2] instanceof Double) || ((Double) a[2]).doubleValue() != 3.5d) {
            return 13;
        }
        if (!(a[3] instanceof Boolean) || !((Boolean) a[3]).booleanValue()) {
            return 14;
        }

        ep.answer = Character.valueOf('z');
        if (pr.morePrims('q', (byte) 7, (short) 300, 1.5f) != 'z') {
            return 15;
        }
        a = ep.lastArgs;
        if (!(a[0] instanceof Character) || ((Character) a[0]).charValue() != 'q') {
            return 16;
        }
        if (!(a[1] instanceof Byte) || ((Byte) a[1]).byteValue() != (byte) 7) {
            return 17;
        }
        if (!(a[2] instanceof Short) || ((Short) a[2]).shortValue() != (short) 300) {
            return 18;
        }
        if (!(a[3] instanceof Float) || ((Float) a[3]).floatValue() != 1.5f) {
            return 19;
        }

        // 20: a void method discards whatever the handler returns, even if it is not null.
        ep.answer = "basura";
        pr.nothing();
        if (!"nada".equals(ep.lastMethod.getName())) {
            return 20;
        }

        // 21..27: each primitive return separately.
        ep.answer = Long.valueOf(9000000000L);
        if (pr.len() != 9000000000L) {
            return 21;
        }
        ep.answer = Double.valueOf(2.25d);
        if (pr.doubleVal() != 2.25d) {
            return 22;
        }
        ep.answer = Float.valueOf(0.5f);
        if (pr.floatVal() != 0.5f) {
            return 23;
        }
        ep.answer = Boolean.TRUE;
        if (!pr.bool()) {
            return 24;
        }
        ep.answer = Byte.valueOf((byte) -3);
        if (pr.octet() != (byte) -3) {
            return 25;
        }
        ep.answer = Short.valueOf((short) -300);
        if (pr.shortVal() != (short) -300) {
            return 26;
        }
        ep.answer = Character.valueOf('k');
        if (pr.character() != 'k') {
            return 27;
        }

        // 28..33: hashCode, equals and toString go to the handler, with the Methods of Object.
        Spy eo = new Spy();
        Object po = newProxy(eo, new Class<?>[] { Greeting.class });
        eo.answer = "I am the proxy";
        if (!"I am the proxy".equals(po.toString())) {
            return 28;
        }
        if (eo.lastMethod.getDeclaringClass() != Object.class) {
            return 29;
        }
        if (!"toString".equals(eo.lastMethod.getName())) {
            return 30;
        }
        eo.answer = Integer.valueOf(777);
        if (po.hashCode() != 777) {
            return 31;
        }
        if (eo.lastMethod.getDeclaringClass() != Object.class
                || !"hashCode".equals(eo.lastMethod.getName())) {
            return 32;
        }
        eo.answer = Boolean.TRUE;
        if (!po.equals("cualquiera")) {
            return 33;
        }
        if (eo.lastMethod.getDeclaringClass() != Object.class
                || !"equals".equals(eo.lastMethod.getName())
                || eo.lastArgs == null || eo.lastArgs.length != 1
                || !"cualquiera".equals(eo.lastArgs[0])) {
            return 34;
        }

        // 35: the same method in two interfaces is implemented once; the Method is that of the
        // first that declares it.
        Spy ed = new Spy();
        ed.answer = "uno";
        Object pd = newProxy(ed, new Class<?>[] { FirstA.class, SecondB.class });
        ((SecondB) pd).name();
        if (ed.lastMethod.getDeclaringClass() != FirstA.class) {
            return 35;
        }
        ((FirstA) pd).name();
        if (ed.lastMethod.getDeclaringClass() != FirstA.class) {
            return 36;
        }

        // 37: covariant returns -> both signatures exist and both dispatch.
        Spy ec = new Spy();
        ec.answer = "cov";
        Object pc = newProxy(ec, new Class<?>[] { CovarObj.class, CovarStr.class });
        if (!"cov".equals(((CovarStr) pc).value())) {
            return 37;
        }
        if (!"cov".equals(((CovarObj) pc).value())) {
            return 38;
        }

        // 39: incompatible returns -> newProxyInstance fails.
        try {
            newProxy(new Spy(), new Class<?>[] { BadA.class, BadB.class });
            return 39;
        } catch (IllegalArgumentException ok) {
            // expected
        }

        // 40: null for a method with a primitive return -> NullPointerException.
        Spy en = new Spy();
        Prims pn = (Prims) newProxy(en, new Class<?>[] { Prims.class });
        en.answer = null;
        try {
            pn.len();
            return 40;
        } catch (NullPointerException ok) {
            // expected
        }

        // 41: an incompatible type -> ClassCastException.
        Spy et = new Spy();
        Greeting ps = (Greeting) newProxy(et, new Class<?>[] { Greeting.class });
        et.answer = Integer.valueOf(5);
        try {
            ps.hello();
            return 41;
        } catch (ClassCastException ok) {
            // expected
        }

        // 42: an incompatible type for a primitive return -> also ClassCastException.
        et.answer = "I am not a long";
        try {
            ((Prims) newProxy(et, new Class<?>[] { Prims.class })).len();
            return 42;
        } catch (ClassCastException ok) {
            // expected
        }

        // 43..45: a checked one the method declares passes; one it does not declare comes out
        // wrapped.
        Spy ex = new Spy();
        Checked pq = (Checked) newProxy(ex, new Class<?>[] { Checked.class });
        java.io.IOException io = new java.io.IOException("io");
        ex.toThrow = io;
        try {
            pq.declared();
            return 43;
        } catch (java.io.IOException ok) {
            if (ok != io) {
                return 44;
            }
        }
        ex.toThrow = io;
        try {
            pq.undeclared();
            return 45;
        } catch (UndeclaredThrowableException ok) {
            if (ok.getUndeclaredThrowable() != io) {
                return 46;
            }
        }

        // 47..48: RuntimeException and Error are never wrapped.
        RuntimeException re = new IllegalStateException("re");
        ex.toThrow = re;
        try {
            pq.undeclared();
            return 47;
        } catch (RuntimeException ok) {
            if (ok != re) {
                return 48;
            }
        }
        Error er = new StackOverflowError("er");
        ex.toThrow = er;
        try {
            pq.undeclared();
            return 49;
        } catch (Error ok) {
            if (ok != er) {
                return 50;
            }
        }

        // 51: two requests with the same interfaces give THE SAME class.
        Class<?> c1 = Proxy.getProxyClass(ProxyTest.class.getClassLoader(),
                new Class<?>[] { Greeting.class });
        Class<?> c2 = Proxy.getProxyClass(ProxyTest.class.getClassLoader(),
                new Class<?>[] { Greeting.class });
        if (c1 != c2) {
            return 51;
        }
        if (c1 != p.getClass()) {
            return 52;
        }
        if (!Proxy.isProxyClass(c1)) {
            return 53;
        }

        // 54: getInterfaces() of the generated class is exactly what was asked for, in order.
        Class<?>[] ifs = pd.getClass().getInterfaces();
        if (ifs.length != 2 || ifs[0] != FirstA.class || ifs[1] != SecondB.class) {
            return 54;
        }

        // 55: the generated class extends Proxy.
        if (!Proxy.class.isAssignableFrom(c1)) {
            return 55;
        }

        // 56..57: arrays as a parameter and as a return.
        Spy ea = new Spy();
        WithArrays pa = (WithArrays) newProxy(ea, new Class<?>[] { WithArrays.class });
        int[] given = new int[] { 1, 2, 3 };
        int[] returned = new int[] { 4, 5 };
        ea.answer = returned;
        if (pa.duplicate(given) != returned) {
            return 56;
        }
        if (ea.lastArgs.length != 1 || ea.lastArgs[0] != given) {
            return 57;
        }
        String[][] m = new String[][] { { "a" } };
        ea.answer = m;
        if (pa.matrix(new String[] { "x" }) != m) {
            return 58;
        }

        // 59: a `default` method is intercepted too -- the proxy does not inherit its body.
        Spy eg = new Spy();
        WithDefault pg = (WithDefault) newProxy(eg, new Class<?>[] { WithDefault.class });
        eg.answer = "interceptado";
        if (!"interceptado".equals(pg.derived())) {
            return 59;
        }

        // 60: a class that is not an interface is an error.
        try {
            newProxy(new Spy(), new Class<?>[] { String.class });
            return 60;
        } catch (IllegalArgumentException ok) {
            // expected
        }

        // 61: the same interface twice is an error.
        try {
            newProxy(new Spy(), new Class<?>[] { Greeting.class, Greeting.class });
            return 61;
        } catch (IllegalArgumentException ok) {
            // expected
        }

        // 62: a null handler is NullPointerException.
        try {
            Proxy.newProxyInstance(ProxyTest.class.getClassLoader(),
                    new Class<?>[] { Greeting.class }, null);
            return 62;
        } catch (NullPointerException ok) {
            // expected
        }

        // 63: getInvocationHandler over something that is not a proxy is IllegalArgumentException.
        try {
            Proxy.getInvocationHandler("I am not a proxy");
            return 63;
        } catch (IllegalArgumentException ok) {
            // expected
        }

        // 64: a proxy with no interface at all is legal and goes on answering the methods of
        // Object.
        Spy ez = new Spy();
        Object pz = newProxy(ez, new Class<?>[0]);
        ez.answer = "vacio";
        if (!"vacio".equals(pz.toString())) {
            return 64;
        }

        // 65: the final methods of Object are NOT intercepted.
        ez.answer = "it should not be used";
        if (pz.getClass() != pz.getClass()) {
            return 65;
        }

        // 66: the Method that arrives for an interface method declares its exceptions.
        ex.answer = null;
        try {
            pq.declared();
        } catch (Throwable ignored) {
            return 66;
        }
        Class<?>[] decl = ex.lastMethod.getExceptionTypes();
        if (decl.length != 1 || decl[0] != java.io.IOException.class) {
            return 67;
        }

        // 68: a method inherited from a superinterface brings the Method of whoever declares it.
        Spy eh = new Spy();
        eh.answer = "her";
        Derived pdv = (Derived) newProxy(eh, new Class<?>[] { Derived.class });
        pdv.inherited();
        if (eh.lastMethod.getDeclaringClass() != Base.class) {
            return 68;
        }
        pdv.own();
        if (eh.lastMethod.getDeclaringClass() != Derived.class) {
            return 69;
        }

        // 70..72: the bridge of a generic interface is ANOTHER method of the proxy, not a
        // forwarding.
        Spy eb = new Spy();
        eb.answer = Integer.valueOf(1);
        Object pb = newProxy(eb, new Class<?>[] { CompInt.class });
        if (((CompInt) pb).cmp(Integer.valueOf(4)) != 1) {
            return 70;
        }
        Class<?>[] pars = eb.lastMethod.getParameterTypes();
        if (pars.length != 1 || pars[0] != Object.class) {
            // The bridge `cmp(Object)` is the only public method CompInt inherits; the version with
            // Integer only exists if the compiler emits it as well.
            if (pars.length != 1 || pars[0] != Integer.class) {
                return 71;
            }
        }
        if (((Comparer) pb).cmp("cualquier cosa") != 1) {
            return 72;
        }

        // 73: an interface that redeclares toString() does not add a second method; the Method of
        // Object goes on arriving.
        Spy es = new Spy();
        es.answer = "ts";
        Object pt = newProxy(es, new Class<?>[] { WithToString.class });
        if (!"ts".equals(pt.toString())) {
            return 73;
        }
        if (es.lastMethod.getDeclaringClass() != Object.class) {
            return 74;
        }

        // 75..76: a non-public interface can be proxied, and the generated class is not public.
        Spy er2 = new Spy();
        er2.answer = "shh";
        Object prv = newProxy(er2, new Class<?>[] { NotPublic.class });
        if (!"shh".equals(((NotPublic) prv).secret())) {
            return 75;
        }
        if (java.lang.reflect.Modifier.isPublic(prv.getClass().getModifiers())) {
            return 76;
        }

        // 77: every proxy is Serializable, because Proxy is.
        if (!(prv instanceof java.io.Serializable)) {
            return 77;
        }

        // 78: two interfaces where the second extends the first is legal.
        Spy ee = new Spy();
        ee.answer = "ok";
        Object pe = newProxy(ee, new Class<?>[] { Base.class, Derived.class });
        if (!"ok".equals(((Base) pe).inherited())) {
            return 78;
        }
        if (ee.lastMethod.getDeclaringClass() != Base.class) {
            return 79;
        }

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
