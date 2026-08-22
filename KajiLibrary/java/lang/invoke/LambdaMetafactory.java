package java.lang.invoke;

import java.lang.invoke.MethodHandles.Lookup;

// The bootstrap behind every lambda and method reference. `x -> x + 1` compiles to an
// `invokedynamic` whose bootstrap is this class; at link time it builds a class implementing the
// functional interface and forwarding to the compiler-generated body. The lambda's class
// therefore does not exist until the first execution of that instruction — which is why a lambda
// has no source-level type and cannot be serialized as itself (see `SerializedLambda`).
//
// KajiLibrary does NOT implement it, and cannot: spinning a class needs a class writer and a
// loader. Our VM already covers the case in Rust — `invokedynamic` recognises this class BY NAME
// and spins the implementing class itself — so nothing is lost by this file being a declaration:
// the VM never loads it. The methods throw, which is the honest answer if anyone calls them
// directly.
public final class LambdaMetafactory {

    // Bit flags for `altMetafactory`, whose whole argument list is encoded rather than typed.
    public static final int FLAG_SERIALIZABLE = 1;
    public static final int FLAG_MARKERS = 2;
    public static final int FLAG_BRIDGES = 4;

    private LambdaMetafactory() {
    }

    // The `Object` in place of `MethodHandles.Lookup` is not a design choice: a NESTED type from
    // another file erases to `Object` in the emitted descriptor anyway (#101), so spelling it
    // `Object` at least makes the source agree with the binary. Allowlisted for the gate.
    public static CallSite metafactory(Lookup caller, String interfaceMethodName,
            MethodType factoryType, MethodType interfaceMethodType, MethodHandle implementation,
            MethodType dynamicMethodType) throws LambdaConversionException {
        throw new UnsupportedOperationException("lambda linkage is done by the VM, not the library");
    }

    public static CallSite altMetafactory(Lookup caller, String interfaceMethodName,
            MethodType factoryType, Object[] args) throws LambdaConversionException {
        throw new UnsupportedOperationException("lambda linkage is done by the VM, not the library");
    }
}
