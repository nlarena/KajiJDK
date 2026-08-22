package java.lang.invoke;

// A call site whose target is read and written with volatile semantics: a change is visible to
// every thread immediately, at the cost of the JVM not being able to inline through it. The
// counterpart to `MutableCallSite`, and the reason both exist — the choice is visibility versus
// speed, made per call site.
public class VolatileCallSite extends CallSite {

    public VolatileCallSite(MethodType type) {
        super(type);
    }

    public VolatileCallSite(MethodHandle target) {
        super(target);
    }

    public final MethodHandle getTarget() {
        return target;
    }

    public void setTarget(MethodHandle newTarget) {
        target = newTarget;
    }

    public final MethodHandle dynamicInvoker() {
        throw new UnsupportedOperationException("no dynamic invoker without a handle factory");
    }
}
