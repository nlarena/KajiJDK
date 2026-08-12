package java.lang.invoke;

// Minimal java.lang.invoke.MethodHandle — a typed, directly-callable reference to a method.
// It carries what a JDK DirectMethodHandle carries: the declaring class, the member name, the
// target descriptor, and the kind (REF_invokeStatic = 6, …). The VM's `invoke` intrinsic reads
// these four fields off the heap to know what to call.
//
// `invoke`/`invokeExact` are **signature-polymorphic** (JVMS §2.9.3): the call site's descriptor
// is the *real* one (e.g. `(Ljava/lang/String;)Ljava/lang/Object;`), not this declared
// `(Object...)Object`. The VM intercepts them in `invokevirtual` before normal resolution, so
// these native bodies never actually run — they exist so the class is complete and verifies.
public class MethodHandle {
    final Class<?> owner;     // declaring class (its Class mirror)
    final String name;        // member name
    final String descriptor;  // target method descriptor
    final int kind;           // reference kind (REF_invokeStatic = 6, …)

    MethodHandle(Class<?> owner, String name, String descriptor, int kind) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.kind = kind;
    }

    public native Object invoke(Object... args) throws Throwable;

    public native Object invokeExact(Object... args) throws Throwable;

    // invokeWithArguments(args): spread the array and call the handle. Unlike `invoke` this is a
    // *regular* (non-polymorphic) method — a fixed `([Ljava/lang/Object;)Ljava/lang/Object;`
    // descriptor — but the spreading + dispatch is a VM operation, so it's intercepted natively.
    public native Object invokeWithArguments(Object... args) throws Throwable;
}
