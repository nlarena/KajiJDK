package java.lang.constant;

// A nominal descriptor for an `invokedynamic` call site: which bootstrap builds it, the name and
// type the instruction carries, and the static arguments handed to the bootstrap. It is the
// `condy` story one level up — same shape, except the bootstrap produces a call site rather than
// a value, which is why this one does NOT implement `ConstantDesc`: a call site is not a
// constant-pool constant, it is an instruction's target.
//
// `resolveCallSiteDesc` degrades honestly — building a live `CallSite` needs `java.lang.invoke`.
public final class DynamicCallSiteDesc {

    private final DirectMethodHandleDesc bootstrapMethod;
    private final String invocationName;
    private final MethodTypeDesc invocationType;
    private final ConstantDesc[] bootstrapArgs;

    private DynamicCallSiteDesc(DirectMethodHandleDesc bootstrapMethod, String invocationName,
            MethodTypeDesc invocationType, ConstantDesc[] bootstrapArgs) {
        // Mirrors the reference constructor's invariant, and — like it — makes the compiler emit
        // the `$assertionsDisabled` guard field and its `static {}` initializer.
        assert invocationName.length() > 0 : "invocation name must be non-empty";
        this.bootstrapMethod = bootstrapMethod;
        this.invocationName = invocationName;
        this.invocationType = invocationType;
        this.bootstrapArgs = bootstrapArgs;
    }

    public static DynamicCallSiteDesc of(DirectMethodHandleDesc bootstrapMethod, String invocationName,
            MethodTypeDesc invocationType, ConstantDesc... bootstrapArgs) {
        return new DynamicCallSiteDesc(bootstrapMethod, invocationName, invocationType, bootstrapArgs);
    }

    public static DynamicCallSiteDesc of(DirectMethodHandleDesc bootstrapMethod, String invocationName,
            MethodTypeDesc invocationType) {
        return new DynamicCallSiteDesc(bootstrapMethod, invocationName, invocationType, new ConstantDesc[0]);
    }

    // The name is only meaningful to some bootstraps; `_` is the conventional placeholder.
    public static DynamicCallSiteDesc of(DirectMethodHandleDesc bootstrapMethod, MethodTypeDesc invocationType) {
        return of(bootstrapMethod, "_", invocationType);
    }

    public DynamicCallSiteDesc withArgs(ConstantDesc... bootstrapArgs) {
        return new DynamicCallSiteDesc(bootstrapMethod, invocationName, invocationType, bootstrapArgs);
    }

    public DynamicCallSiteDesc withNameAndType(String invocationName, MethodTypeDesc invocationType) {
        return new DynamicCallSiteDesc(bootstrapMethod, invocationName, invocationType, bootstrapArgs);
    }

    public String invocationName() {
        return invocationName;
    }

    public MethodTypeDesc invocationType() {
        return invocationType;
    }

    public MethodHandleDesc bootstrapMethod() {
        // The cast is the sibling of #120: `DirectMethodHandleDesc extends MethodHandleDesc`, but
        // the check of one CLASSPATH interface against another rejects the widening. Spelling it
        // out satisfies the compiler and is a no-op in the bytecode's eyes.
        return (MethodHandleDesc) bootstrapMethod;
    }

    public ConstantDesc[] bootstrapArgs() {
        ConstantDesc[] copy = new ConstantDesc[bootstrapArgs.length];
        int i = 0;
        while (i < bootstrapArgs.length) {
            copy[i] = bootstrapArgs[i];
            i = i + 1;
        }
        return copy;
    }

    public final boolean equals(Object o) {
        boolean same = false;
        if (o instanceof DynamicCallSiteDesc) {
            DynamicCallSiteDesc other = (DynamicCallSiteDesc) o;
            same = invocationName.equals(other.invocationName)
                    && invocationType.equals(other.invocationType)
                    && bootstrapMethod.equals(other.bootstrapMethod);
        }
        return same;
    }

    public final int hashCode() {
        return (invocationName.hashCode() * 31 + invocationType.hashCode()) * 31
                + bootstrapMethod.hashCode();
    }

    public String toString() {
        return "DynamicCallSiteDesc[" + invocationName + invocationType.displayDescriptor() + "]";
    }

    /**
     * Unsupported: resolving a call site needs `java.lang.invoke`, which this library does not
     * carry. Everything descriptive about the call site works without it.
     *
     * @param lookup the lookup that would perform the resolution
     * @throws UnsupportedOperationException always
     */
    public java.lang.invoke.CallSite resolveCallSiteDesc(java.lang.invoke.MethodHandles.Lookup lookup) {
        throw new UnsupportedOperationException("resolution needs java.lang.invoke");
    }
}
