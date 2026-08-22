package java.lang.invoke;

// Wraps a method handle in an instance of a single-method interface — the inverse of what
// `Lookup.unreflect` does. It is how a handle becomes something ordinary code can call through a
// normal interface, and it is what makes a handle usable as a callback without every caller
// knowing about `java.lang.invoke`.
//
// Needs to generate a class at run time, which is a VM capability, so the methods throw.
public final class MethodHandleProxies {

    private MethodHandleProxies() {
    }

    public static <T> T asInterfaceInstance(Class<T> intfc, MethodHandle target) {
        throw new UnsupportedOperationException("proxy generation needs VM support");
    }

    public static boolean isWrapperInstance(Object x) {
        return false;
    }

    public static MethodHandle wrapperInstanceTarget(Object x) {
        throw new IllegalArgumentException("not a wrapper instance");
    }

    public static Class<?> wrapperInstanceType(Object x) {
        throw new IllegalArgumentException("not a wrapper instance");
    }
}
