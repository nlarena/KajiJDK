package java.lang.invoke;

// The factory for method handles, and the home of `Lookup` — the object that carries ACCESS
// RIGHTS. That is the part worth understanding: a handle is not obtained from a class but from a
// lookup, and the lookup remembers which class asked for it. `MethodHandles.lookup()` captures
// the caller, so the handles it produces reach exactly what the caller could reach by writing the
// call directly. Access control therefore happens ONCE, when the handle is created, instead of on
// every invocation — which is what lets a handle be as fast as a direct call.
//
// Capturing the caller means walking the stack, a VM operation, and every factory below has to
// produce a `MethodHandle`, which nothing in the library can build. So this is a declaration with
// honest holes.
//
// OMITTED (subset), and it is most of the class: the ~40 combinators (`filterArguments`,
// `foldArguments`, `guardWithTest`, `catchException`, the `loop` family, …), the `VarHandle`
// factories, `privateLookupIn` and the `byte*ViewVarHandle` pair that needs `java.nio.ByteOrder`.
// They are all shapes over handles, and a shape over something that cannot exist would add
// nothing but more surface to be wrong about.
public final class MethodHandles {

    private MethodHandles() {
    }

    public static Lookup lookup() {
        throw new UnsupportedOperationException("a Lookup captures its caller, which needs VM support");
    }

    public static Lookup publicLookup() {
        throw new UnsupportedOperationException("a Lookup captures its caller, which needs VM support");
    }

    public static MethodHandle arrayConstructor(Class<?> arrayClass) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle arrayLength(Class<?> arrayClass) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle arrayElementGetter(Class<?> arrayClass) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle arrayElementSetter(Class<?> arrayClass) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle exactInvoker(MethodType type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle invoker(MethodType type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle spreadInvoker(MethodType type, int leadingArgCount) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle explicitCastArguments(MethodHandle target, MethodType newType) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle constant(Class<?> type, Object value) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle identity(Class<?> type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle zero(Class<?> type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle empty(MethodType type) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    public static MethodHandle throwException(Class<?> returnType, Class<?> exType) {
        throw new UnsupportedOperationException("no method handle factory without VM support");
    }

    // The access-rights token. Nested, as in the JDK, because its identity is inseparable from the
    // factory that hands it out.
    public static final class Lookup {

        // Which accesses this lookup may perform. A lookup can be NARROWED but never widened,
        // which is what makes it safe to hand one to somebody else.
        public static final int PUBLIC = 1;
        public static final int PRIVATE = 2;
        public static final int PROTECTED = 4;
        public static final int PACKAGE = 8;
        public static final int MODULE = 16;
        public static final int UNCONDITIONAL = 32;
        public static final int ORIGINAL = 64;

        Lookup() {
        }

        public Class<?> lookupClass() {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public int lookupModes() {
            return 0;
        }

        public MethodHandle findStatic(Class<?> refc, String name, MethodType type)
                throws NoSuchMethodException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findVirtual(Class<?> refc, String name, MethodType type)
                throws NoSuchMethodException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findConstructor(Class<?> refc, MethodType type)
                throws NoSuchMethodException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findGetter(Class<?> refc, String name, Class<?> type)
                throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findSetter(Class<?> refc, String name, Class<?> type)
                throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findStaticGetter(Class<?> refc, String name, Class<?> type)
                throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public MethodHandle findStaticSetter(Class<?> refc, String name, Class<?> type)
                throws NoSuchFieldException, IllegalAccessException {
            throw new UnsupportedOperationException("no Lookup without VM support");
        }

        public String toString() {
            return "Lookup";
        }
    }
}
