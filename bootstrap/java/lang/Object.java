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

    // Monitor signalling — the condition-variable half of `synchronized`. All native:
    // they manipulate the VM's scheduler (suspend/wake threads on this object's
    // monitor), which plain bytecode can't reach. Must be called holding the monitor.
    public final native void wait();    // release the monitor + sleep until notified
    public final native void notify();    // wake one waiter
    public final native void notifyAll(); // wake all waiters
}
