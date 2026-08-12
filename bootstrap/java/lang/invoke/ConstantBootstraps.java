package java.lang.invoke;

// Minimal java.lang.invoke.ConstantBootstraps — the bootstrap methods for dynamic constants
// (condy). `invoke` is the only one javac emits: it means "call this MethodHandle with these
// arguments and use the result as the constant". Now that MethodHandle invocation is a VM
// primitive, this is *library* code, in Java, no longer a Rust intrinsic — the whole point of
// giving the VM `MethodHandle.invoke`.
public final class ConstantBootstraps {
    private ConstantBootstraps() {
    }

    public static Object invoke(MethodHandles.Lookup lookup, String name, Class<?> type,
                                MethodHandle handle, Object... args) throws Throwable {
        return handle.invokeWithArguments(args);
    }
}
