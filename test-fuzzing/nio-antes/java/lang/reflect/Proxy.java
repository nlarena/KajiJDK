package java.lang.reflect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import jdk.internal.reflect.ProxyDispatcher;

/**
 * KajiLibrary's java.lang.reflect.Proxy.
 *
 * <p>A proxy class is a class that is <em>fabricated at run time</em>: given a list of interfaces
 * and an {@link InvocationHandler}, {@link #newProxyInstance} builds a real {@code .class} in
 * memory, hands it to the VM, and returns an instance of it. Every method of every interface on
 * that instance ends up as one call to {@code handler.invoke(proxy, method, args)}.
 *
 * <p>That is not a shape or a stand-in -- there is no other way to get a type that implements an
 * interface nobody wrote an implementation for. The bytes are emitted by
 * {@link ProxyGenerator} (which explains its own format choices) and enter the VM through
 * {@link ClassLoader#defineClass(String, byte[], int, int)}.
 *
 * <h2>One loader</h2>
 *
 * <p>The {@code loader} argument is accepted and ignored, and the cache is keyed on the interface
 * list alone. That is not a shortcut -- KajiJDK has one loader (see {@link ClassLoader}), so two
 * requests for the same interfaces genuinely cannot produce two distinct types here. In a VM with
 * a real delegation hierarchy they could, and that is exactly the difference the parameter
 * encodes; with one loader there is nothing for it to select.
 *
 * <h2>What a proxy does NOT intercept</h2>
 *
 * <p>{@code hashCode}, {@code equals} and {@code toString} go to the handler, with the
 * {@link Method} objects of {@link Object}. Every other method of {@code Object} is {@code final}
 * and keeps its inherited behaviour. Static and private interface methods are not intercepted
 * either: they are not inherited, so there is nothing to override.
 */
public class Proxy implements Serializable {

    private static final long serialVersionUID = -2222568056686623797L;

    /** The invocation handler every call on this proxy is routed to. */
    protected InvocationHandler h;

    // ---- the factory's global state ----
    //
    // One lock for the cache and the name counter: both are touched once per proxy CLASS, not once
    // per instance, so there is nothing to gain by making them finer.

    private static final Object LOCK = new Object();

    /** From the list of interfaces (their names, in order) to the generated class. */
    private static final HashMap<String, Class<?>> CACHE = new HashMap<String, Class<?>>();

    /**
     * The classes this factory generated, so {@link #isProxyClass} does not have to guess.
     *
     * <p>Concurrent and outside the lock on purpose: {@link #getInvocationHandler} consults it, and
     * {@code getInvocationHandler} is on the path of EVERY call to EVERY proxy in the program. A
     * global lock there would turn every proxy into a serialisation point. It is written once per
     * class, with the lock held all the same.
     */
    private static final ConcurrentHashMap<Class<?>, Boolean> GENERADAS =
            new ConcurrentHashMap<Class<?>, Boolean>();

    private static int nextNumber;

    /**
     * The loader that defines the generated classes.
     *
     * <p>`defineClass` is `protected`: the only way to reach it is from a subclass, and that is why
     * there is one. It is not a delegation hierarchy -- it is the permission, written as
     * inheritance.
     */
    private static final class ProxyLoader extends ClassLoader {
        Class<?> define(String name, byte[] bytes) {
            return this.defineClass(name, bytes, 0, bytes.length);
        }
    }

    private static final ProxyLoader LOADER = new ProxyLoader();

    /**
     * The constructor a generated proxy class calls.
     *
     * @param h the handler to route calls to
     */
    protected Proxy(InvocationHandler h) {
        if (h == null) {
            throw new NullPointerException("h");
        }
        this.h = h;
    }

    // No handler, no proxy: it exists so nobody inherits from Proxy and skips the argument.
    private Proxy() {
    }

    /**
     * Returns the proxy class for {@code interfaces}, generating it on first request.
     *
     * @param loader ignored -- see the class notes on KajiJDK's single loader
     * @param interfaces the interfaces the class must implement, in order
     * @return the proxy class
     * @throws IllegalArgumentException if the interface list is not legal
     * @deprecated Prefer {@link #newProxyInstance}, which does not expose the generated class.
     */
    @Deprecated
    public static Class<?> getProxyClass(ClassLoader loader, Class<?>... interfaces)
            throws IllegalArgumentException {
        return Proxy.proxyClass(Proxy.copyAndValidate(interfaces));
    }

    /**
     * Creates a proxy instance for {@code interfaces} that routes every call to {@code h}.
     *
     * @param loader ignored -- see the class notes on KajiJDK's single loader
     * @param interfaces the interfaces the proxy must implement, in order
     * @param h the handler
     * @return the new proxy instance
     * @throws IllegalArgumentException if the interface list is not legal
     * @throws NullPointerException if {@code interfaces} or {@code h} is null
     */
    public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces,
            InvocationHandler h) {
        if (h == null) {
            throw new NullPointerException("h");
        }
        Class<?>[] copy = Proxy.copyAndValidate(interfaces);
        Class<?> clazz = Proxy.proxyClass(copy);
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor(
                    new Class<?>[] { InvocationHandler.class });
            ctor.setAccessible(true);
            return ctor.newInstance(new Object[] { h });
        } catch (InvocationTargetException failure) {
            // The generated constructor is `super(h)` and nothing else; if that throws, the error
            // comes from Proxy and not from the user, and handing it back bare is the only useful
            // thing.
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new InternalError(failure.toString());
        } catch (NoSuchMethodException impossible) {
            throw new InternalError(impossible.toString());
        }
    }

    /**
     * Whether {@code cl} is a class this factory generated.
     *
     * @param cl the class to test
     * @return true if it is a proxy class
     */
    public static boolean isProxyClass(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException("cl");
        }
        return Proxy.GENERADAS.containsKey(cl);
    }

    /**
     * The handler of a proxy instance.
     *
     * @param proxy the proxy instance
     * @return its handler
     * @throws IllegalArgumentException if {@code proxy} is not a proxy instance
     */
    public static InvocationHandler getInvocationHandler(Object proxy)
            throws IllegalArgumentException {
        if (!Proxy.isProxyClass(proxy.getClass())) {
            throw new IllegalArgumentException("not a proxy instance");
        }
        return ((Proxy) proxy).h;
    }

    // ------------------------------------------------------------------ factory

    /**
     * It copies the array of interfaces and validates it.
     *
     * <p>The copy is not paranoia: between validating and generating there is reflection over each
     * element, and an array the caller can go on writing turns "I already validated it" into a
     * lie.
     */
    private static Class<?>[] copyAndValidate(Class<?>[] interfaces) {
        if (interfaces == null) {
            throw new NullPointerException("interfaces");
        }
        Class<?>[] copy = new Class<?>[interfaces.length];
        System.arraycopy(interfaces, 0, copy, 0, interfaces.length);
        if (copy.length > 65535) {
            throw new IllegalArgumentException("interface limit exceeded: " + copy.length);
        }
        int i = 0;
        while (i < copy.length) {
            Class<?> intf = copy[i];
            if (intf == null) {
                throw new NullPointerException("interfaces[" + i + "]");
            }
            if (!intf.isInterface()) {
                throw new IllegalArgumentException(intf.getName() + " is not an interface");
            }
            int j = 0;
            while (j < i) {
                if (copy[j] == intf) {
                    throw new IllegalArgumentException("repeated interface: " + intf.getName());
                }
                j = j + 1;
            }
            i = i + 1;
        }
        return copy;
    }

    private static Class<?> proxyClass(Class<?>[] interfaces) {
        String key = Proxy.key(interfaces);
        synchronized (Proxy.LOCK) {
            Class<?> seen = Proxy.CACHE.get(key);
            if (seen != null) {
                return seen;
            }
            Class<?> fresh = Proxy.generate(interfaces);
            Proxy.CACHE.put(key, fresh);
            Proxy.GENERADAS.put(fresh, Boolean.TRUE);
            return fresh;
        }
    }

    /**
     * The cache's key: the interfaces' names in order, separated by a character that cannot appear
     * in a binary name -- otherwise `{A, BC}` and `{AB, C}` would collide.
     */
    private static String key(Class<?>[] interfaces) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < interfaces.length) {
            sb.append(interfaces[i].getName());
            sb.append(';');
            i = i + 1;
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ the method table

    /**
     * One of the proxy's methods while it is being built: the `Method` the handler will receive,
     * its descriptor, and the exceptions it declares after merging every interface that declares it.
     */
    private static final class ProxyMethod {
        final Method method;
        final String shortSignature;
        final Class<?> returnType;
        Class<?>[] exceptions;

        ProxyMethod(Method method, String shortSignature) {
            this.method = method;
            this.shortSignature = shortSignature;
            this.returnType = method.getReturnType();
            this.exceptions = method.getExceptionTypes();
        }
    }

    private static Class<?> generate(Class<?>[] interfaces) {
        ArrayList<ProxyMethod> entries = new ArrayList<ProxyMethod>();
        // From the full signature (name + parameters + return) to the entry already created. It is
        // what makes a method declared by two interfaces implemented ONCE -- and, since the first to
        // declare it is the one that left the entry, it is also what decides which `Method` the
        // handler receives.
        HashMap<String, ProxyMethod> bySignature = new HashMap<String, ProxyMethod>();

        // Object first: hashCode, equals and toString are the three methods of Object that are not
        // final, and that is why they are the three -- and the only three -- a proxy can
        // intercept.
        Proxy.add(entries, bySignature, Proxy.objectMethod("hashCode", new Class<?>[0]));
        Proxy.add(entries, bySignature,
                Proxy.objectMethod("equals", new Class<?>[] { Object.class }));
        Proxy.add(entries, bySignature, Proxy.objectMethod("toString", new Class<?>[0]));

        int i = 0;
        while (i < interfaces.length) {
            Method[] publicMethods = interfaces[i].getMethods();
            int j = 0;
            while (j < publicMethods.length) {
                Method m = publicMethods[j];
                // An interface's statics are not inherited: there is nothing to override.
                if (!Modifier.isStatic(m.getModifiers())) {
                    Proxy.add(entries, bySignature, m);
                }
                j = j + 1;
            }
            i = i + 1;
        }

        Proxy.checkReturns(entries);

        Method[] methods = new Method[entries.size()];
        Class<?>[][] declared = new Class<?>[entries.size()][];
        i = 0;
        while (i < entries.size()) {
            methods[i] = entries.get(i).method;
            declared[i] = entries.get(i).exceptions;
            i = i + 1;
        }

        boolean isPublic = Proxy.allPublic(interfaces);
        String packageOf = Proxy.packageOf(interfaces, isPublic);
        String name;
        synchronized (Proxy.LOCK) {
            // With no package the dot does not go in: "$Proxy0" is a valid binary name, ".$Proxy0"
            // is not.
            String prefix = packageOf.isEmpty() ? "" : packageOf + ".";
            name = prefix + "$Proxy" + Proxy.nextNumber;
            Proxy.nextNumber = Proxy.nextNumber + 1;
        }

        byte[] bytes = ProxyGenerator.generate(name, interfaces, isPublic, methods);
        Class<?> clazz = Proxy.LOADER.define(name, bytes);
        // Register BEFORE the first instance exists: in this gap the class is already defined but
        // nobody can invoke it yet.
        ProxyDispatcher.register(clazz, methods, declared);
        return clazz;
    }

    private static Method objectMethod(String name, Class<?>[] params) {
        try {
            return Object.class.getMethod(name, params);
        } catch (NoSuchMethodException impossible) {
            throw new InternalError(impossible.toString());
        }
    }

    private static void add(ArrayList<ProxyMethod> entries, HashMap<String, ProxyMethod> bySignature,
            Method m) {
        String shortSig = Proxy.shortSignature(m);
        String full = shortSig + ProxyGenerator.descriptor(m.getReturnType());
        ProxyMethod seen = bySignature.get(full);
        if (seen != null) {
            // The same method a second time: the entry is not duplicated, but the exceptions are
            // merged. A proxy can only declare what ALL the interfaces allow -- if one declares
            // IOException and the other declares nothing, the proxy cannot throw IOException.
            seen.exceptions = Proxy.mergeExceptions(seen.exceptions, m.getExceptionTypes());
            return;
        }
        ProxyMethod fresh = new ProxyMethod(m, shortSig);
        bySignature.put(full, fresh);
        entries.add(fresh);
    }

    /** Name + the parameters' descriptors: what defines "the same method" when overriding. */
    private static String shortSignature(Method m) {
        Class<?>[] params = m.getParameterTypes();
        StringBuilder sb = new StringBuilder(m.getName());
        sb.append('(');
        int i = 0;
        while (i < params.length) {
            sb.append(ProxyGenerator.descriptor(params[i]));
            i = i + 1;
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * The useful intersection of two lists of declared exceptions: from each list, what the other
     * already covers. It is not the literal intersection -- if one declares `IOException` and the
     * other `FileNotFoundException`, what is left is `FileNotFoundException`, which is the only
     * thing both signatures admit.
     */
    private static Class<?>[] mergeExceptions(Class<?>[] a, Class<?>[] b) {
        ArrayList<Class<?>> out = new ArrayList<Class<?>>();
        Proxy.collectCovered(a, b, out);
        Proxy.collectCovered(b, a, out);
        return out.toArray(new Class<?>[out.size()]);
    }

    private static void collectCovered(Class<?>[] from, Class<?>[] against,
            ArrayList<Class<?>> out) {
        int i = 0;
        while (i < from.length) {
            int j = 0;
            while (j < against.length) {
                if (against[j].isAssignableFrom(from[i])) {
                    if (!out.contains(from[i])) {
                        out.add(from[i]);
                    }
                    j = against.length;
                } else {
                    j = j + 1;
                }
            }
            i = i + 1;
        }
    }

    /**
     * It rejects two interfaces declaring the same method with returns that cannot be reconciled.
     *
     * <p>DIFFERENT returns are not an error in themselves: a class file admits two methods with the
     * same name, the same parameters and different returns, and that is what gets generated for
     * covariant returns (`Object value()` and `String value()` coexist). What cannot happen is that
     * none of the returns covers the others -- `String` and `Integer` have no method serving both
     * --, and there the request is impossible and has to be called out before anything is generated.
     * A primitive never covers and is never covered, so one appearing in a group of more than one is
     * enough to make the group impossible.
     */
    private static void checkReturns(ArrayList<ProxyMethod> entries) {
        HashMap<String, ArrayList<ProxyMethod>> groups = new HashMap<String, ArrayList<ProxyMethod>>();
        int i = 0;
        while (i < entries.size()) {
            ProxyMethod e = entries.get(i);
            ArrayList<ProxyMethod> group = groups.get(e.shortSignature);
            if (group == null) {
                group = new ArrayList<ProxyMethod>();
                groups.put(e.shortSignature, group);
            }
            group.add(e);
            i = i + 1;
        }
        java.util.Iterator<ArrayList<ProxyMethod>> it = groups.values().iterator();
        while (it.hasNext()) {
            ArrayList<ProxyMethod> group = it.next();
            if (group.size() < 2) {
                continue;
            }
            ArrayList<Class<?>> uncovered = new ArrayList<Class<?>>();
            int k = 0;
            while (k < group.size()) {
                Class<?> fresh = group.get(k).returnType;
                if (fresh.isPrimitive()) {
                    throw new IllegalArgumentException("methods with same signature "
                            + group.get(0).shortSignature + " but incompatible return types");
                }
                boolean covered = false;
                int t = 0;
                while (t < uncovered.size()) {
                    Class<?> old = uncovered.get(t);
                    if (fresh.isAssignableFrom(old)) {
                        covered = true;
                        t = uncovered.size();
                    } else if (old.isAssignableFrom(fresh)) {
                        uncovered.remove(t);
                    } else {
                        t = t + 1;
                    }
                }
                if (!covered) {
                    uncovered.add(fresh);
                }
                k = k + 1;
            }
            if (uncovered.size() > 1) {
                throw new IllegalArgumentException("methods with same signature "
                        + group.get(0).shortSignature + " but incompatible return types");
            }
        }
    }

    private static boolean allPublic(Class<?>[] interfaces) {
        int i = 0;
        while (i < interfaces.length) {
            if (!Modifier.isPublic(interfaces[i].getModifiers())) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * The generated class's package.
     *
     * <p>With every interface public it goes into one of its own and the class is public. With one
     * that is not, the class HAS to land in that interface's package -- a package-private interface
     * cannot be implemented from outside --, and that is why two non-public interfaces from
     * different packages cannot be proxied together: there is no package where both are visible.
     */
    private static String packageOf(Class<?>[] interfaces, boolean isPublic) {
        if (isPublic) {
            return "com.sun.proxy";
        }
        String chosen = null;
        int i = 0;
        while (i < interfaces.length) {
            if (!Modifier.isPublic(interfaces[i].getModifiers())) {
                String p = interfaces[i].getPackageName();
                if (chosen == null) {
                    chosen = p;
                } else if (!chosen.equals(p)) {
                    throw new IllegalArgumentException(
                            "non-public interfaces from different packages");
                }
            }
            i = i + 1;
        }
        return chosen;
    }
}
