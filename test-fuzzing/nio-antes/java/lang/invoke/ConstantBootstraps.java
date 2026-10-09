package java.lang.invoke;

// The bootstraps behind `condy` — a constant the JVM computes on first use by CALLING a method,
// instead of reading it from the pool. These are the standard ones the language itself emits:
// a null of a given type, a primitive class, an enum constant, a static final field's value.
//
// The pattern to notice is that every one takes the same three leading parameters — lookup, name,
// type — because that is the fixed prefix the JVM passes to ANY bootstrap. Everything after it
// is per-bootstrap static arguments from the constant pool.
//
// Our VM implements these in Rust and recognises the class by name, so this declaration is never
// loaded.
//
// SPELLING — `MethodHandles$Lookup` rather than `MethodHandles.Lookup`. This file is where
// defect #208 was first seen and it is worth stating what it was, because the fix is not
// obvious. `MethodHandles.Lookup` does not resolve at all (#101), so an earlier revision reached
// for `import java.lang.invoke.MethodHandles.Lookup` plus the simple name — which compiles, and
// emits the descriptor `LLookup;`: a class that exists in NO package. Every method here linked
// against a type that could never be loaded. The type's BINARY name resolves correctly instead,
// because our compiler reads `MethodHandles$Lookup.class` off the classpath as an ordinary member
// of the package — and so does the reference `javac`, under the same one-file-at-a-time,
// `-cp KajiLibrary` build this library uses. See the longer note in `MethodHandles.java`.
//
// `enumConstant` returns the raw bound `Enum` rather than the JDK's `<E extends Enum<E>> E`, for
// the reason `TypeDescriptor.java` gives at length: our compiler erases a BOUNDED type variable
// to `Object` instead of to its leftmost bound (#100), so writing the JDK's generic signature
// would emit `()Ljava/lang/Object;` where the JDK emits `()Ljava/lang/Enum;`. Declaring the bound
// directly gives the JDK's descriptor exactly; the source loses the compile-time precision and
// the BINARY is faithful, which is the trade this library has already made elsewhere.
public final class ConstantBootstraps {

    private ConstantBootstraps() {
    }

    public static Object nullConstant(MethodHandles$Lookup lookup, String name, Class<?> type) {
        return null;
    }

    public static Class<?> primitiveClass(MethodHandles$Lookup lookup, String name, Class<?> type) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    // The constant is named by the `name` parameter — the field name of the enum constant — and
    // `type` is the enum class. So this bootstrap is the whole of `Enum.valueOf` moved to link
    // time: the lookup happens once, when the constant is first used, and never again.
    public static Enum enumConstant(MethodHandles$Lookup lookup, String name, Class<?> type) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    public static Object getStaticFinal(MethodHandles$Lookup lookup, String name, Class<?> type,
            Class<?> declaringClass) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    public static Object getStaticFinal(MethodHandles$Lookup lookup, String name, Class<?> type) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    public static Object invoke(MethodHandles$Lookup lookup, String name, Class<?> type,
            MethodHandle handle, Object... args) throws Throwable {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    // The shared tail of the bootstraps that adapt a handle's result to the requested type: run the
    // handle, then coerce. Package-private in the reference, and — like everything here — the VM
    // does the real work, so this declaration only fixes the descriptor the JDK exposes.
    static Object makeConstant(MethodHandle handle, String name, Class<?> type, Object value,
            Class<?> declaring) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    public static Object explicitCast(MethodHandles$Lookup lookup, String name, Class<?> type,
            Object value) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    // ---- the VarHandle bootstraps ----
    //
    // A `VarHandle` is an obvious thing to want as a CONSTANT: it is immutable, it is expensive to
    // produce, and the field it names is fixed at compile time. These three are how a class file
    // asks for one without running any code of its own — `condy` computes it on first use and the
    // pool caches it from then on.
    //
    // The three differ only in what has to be named to locate the variable: an array needs its
    // array type, an instance field needs the declaring class and the field's type, and a static
    // field needs the same pair. `type` — the third parameter, the bootstrap's fixed prefix — is
    // `VarHandle.class` in all three and carries no information; that redundancy is the price of
    // the uniform bootstrap shape.

    public static VarHandle arrayVarHandle(MethodHandles$Lookup lookup, String name, Class<?> type,
            Class<?> arrayClass) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    public static VarHandle fieldVarHandle(MethodHandles$Lookup lookup, String name, Class<?> type,
            Class<?> declaringClass, Class<?> fieldType) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }

    public static VarHandle staticFieldVarHandle(MethodHandles$Lookup lookup, String name,
            Class<?> type, Class<?> declaringClass, Class<?> fieldType) {
        throw new UnsupportedOperationException("condy linkage is done by the VM");
    }
}
