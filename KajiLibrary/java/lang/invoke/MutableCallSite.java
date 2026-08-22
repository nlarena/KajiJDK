package java.lang.invoke;

// A call site whose target may be replaced. There is deliberately NO memory-ordering guarantee:
// another thread may keep using the old target for a while, and `syncAll` is what forces the
// change to become visible everywhere. Trading that guarantee away is what keeps the common,
// already-linked case as cheap as a final field.
public class MutableCallSite extends CallSite {

    public MutableCallSite(MethodType type) {
        super(type);
    }

    public MutableCallSite(MethodHandle target) {
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

    // Publishes the pending target changes of every site given. In the JDK this is a VM-level
    // safepoint operation; with nothing to publish here, it is a no-op that keeps the contract.
    public static void syncAll(MutableCallSite[] sites) {
    }
}
