package java.lang;

// Our own minimal java.lang.Object — the real root, replacing the JDK's via
// --patch-module at compile time and our classpath at run time.
public class Object {
    public Object() {
    }

    // getClass() is the purest intrinsic: it reads the object's header (its class_id),
    // which plain bytecode cannot do — only the VM knows where a class lives. Native,
    // so the interpreter dispatches it to the bridge, which returns the Class mirror.
    public final native Class<?> getClass();

    // Identity hash code — the VM's notion of the object's identity (Java can't
    // compute it itself). We use the object's heap offset.
    public native int hashCode();

    // Reference equality by default — the base contract every value type refines
    // (`String`, boxed primitives, records, hash-map keys override it). Not native:
    // the default *is* just identity (`==`); overrides supply value semantics.
    public boolean equals(Object obj) {
        return this == obj;
    }

    // Shallow copy — a new object of the receiver's *runtime* class with every field
    // copied verbatim (references included: original and clone then share the pointees).
    // Native: only the VM knows the object's size and layout, and the copy must bypass
    // constructors. `protected` + the Cloneable opt-in (JLS §10.7): the VM throws
    // CloneNotSupportedException for a receiver whose class doesn't implement Cloneable.
    protected native Object clone() throws CloneNotSupportedException;

    // Monitor signalling — the condition-variable half of `synchronized`. All native:
    // they manipulate the VM's scheduler (suspend/wake threads on this object's
    // monitor), which plain bytecode can't reach. Must be called holding the monitor.
    // `throws InterruptedException`: the runtime intrinsic raises it when the waiter is
    // interrupted, so the signature must say so — otherwise a caller can't legally catch it.
    public final native void wait() throws InterruptedException;        // release the monitor + sleep until notified
    public final native void wait(long ms) throws InterruptedException; // ...or return after `ms` ms even without notify
    public final native void notify();      // wake one waiter
    public final native void notifyAll();   // wake all waiters
}
