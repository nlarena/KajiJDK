package java.lang.invoke;

// The mutable link behind an `invokedynamic` instruction. The instruction is linked ONCE, by its
// bootstrap method, and what the bootstrap returns is a call site; from then on the instruction
// invokes whatever handle the call site currently holds. That indirection is the whole feature —
// it is how a lambda is created lazily, and how a language on the JVM can re-link a call as it
// learns more.
//
// The three subclasses are the three answers to "may the target change, and who needs to see it":
// never (`ConstantCallSite`), yes with no ordering promise (`MutableCallSite`), and yes visible
// immediately to every thread (`VolatileCallSite`).
public abstract class CallSite {

    // Package-private, as in the JDK: only the three subclasses may build one.
    final MethodType targetType;
    MethodHandle target;

    CallSite(MethodType type) {
        this.targetType = type;
    }

    CallSite(MethodHandle target) {
        this.targetType = target.type();
        this.target = target;
    }

    public MethodType type() {
        return targetType;
    }

    public abstract MethodHandle getTarget();

    public abstract void setTarget(MethodHandle newTarget);

    // A handle that always invokes the CURRENT target — the reader's view of the indirection.
    // Producing one needs a handle factory, which is what KajiLibrary lacks.
    public abstract MethodHandle dynamicInvoker();
}
