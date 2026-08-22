package java.lang.invoke;

import java.lang.invoke.MethodHandles.Lookup;

// The bootstraps behind `condy` — a constant the JVM computes on first use by CALLING a method,
// instead of reading it from the pool. These are the standard ones the language itself emits:
// a null of a given type, a primitive class, an enum constant, a static final field's value.
//
// The pattern to notice is that every one takes the same three leading parameters — lookup, name,
// type — because that is the fixed prefix the JVM passes to ANY bootstrap. Everything after it
// is per-bootstrap static arguments from the constant pool.
//
// Our VM implements these in Rust and recognises the class by name, so this declaration is never
// loaded. `Object` stands in for `MethodHandles.Lookup` for the reason given in
// `LambdaMetafactory`.
//
// OMITTED (subset): `enumConstant`, whose type variable is bounded (`E extends Enum<E>`) and
// would erase wrongly (#100), and the three `VarHandle` factories, which need `VarHandle` to be
// more than a shell.
public final class ConstantBootstraps {

    private ConstantBootstraps() {
    }

    public static Object nullConstant(Lookup lookup, String name, Class<?> type) {
        return null;
    }

    public static Class<?> primitiveClass(Lookup lookup, String name, Class<?> type) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    public static Object getStaticFinal(Lookup lookup, String name, Class<?> type, Class<?> declaringClass) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    public static Object getStaticFinal(Lookup lookup, String name, Class<?> type) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    public static Object invoke(Lookup lookup, String name, Class<?> type, MethodHandle handle,
            Object[] args) throws Throwable {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    public static Object explicitCast(Lookup lookup, String name, Class<?> type, Object value) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }
}
