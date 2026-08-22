package java.lang.invoke;

// A call site whose target never changes. The JVM may therefore inline through it permanently,
// which is why `setTarget` does not merely fail to help — it throws.
public class ConstantCallSite extends CallSite {

    public ConstantCallSite(MethodHandle target) {
        super(target);
    }

    // The two-argument form exists for a self-referential bootstrap: a target that needs the call
    // site itself. It is `protected` because only a subclass can complete that dance.
    protected ConstantCallSite(MethodType targetType, MethodHandle createTargetHook) throws Throwable {
        super(targetType);
    }

    public final MethodHandle getTarget() {
        return target;
    }

    public final void setTarget(MethodHandle ignore) {
        throw new UnsupportedOperationException("ConstantCallSite");
    }

    public final MethodHandle dynamicInvoker() {
        throw new UnsupportedOperationException("no dynamic invoker without a handle factory");
    }
}
