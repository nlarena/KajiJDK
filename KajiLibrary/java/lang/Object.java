package java.lang;

// KajiLibrary's java.lang.Object — the root of every class, written from scratch for
// KajiJDK (not inherited from bootstrap/). Methods the VM must implement are `native`
// — getClass reads the object header, hashCode and the monitor ops touch the VM — and
// everything expressible in plain Java is plain Java.
public class Object {

    public Object() {
    }

    // The runtime class of this object: the VM reads it from the object header (its
    // class id), which bytecode cannot do. Pure intrinsic.
    public final native Class<?> getClass();

    // Identity hash code — the VM's notion of the object's identity, which Java cannot
    // compute itself (we use the heap offset).
    public native int hashCode();

    // Reference equality by default (§java.lang.Object): two references are equal only
    // if they denote the same object. Subclasses override this for value equality.
    public boolean equals(Object obj) {
        return this == obj;
    }

    // The root string representation: the class name and the identity hash in hex, e.g.
    // "java.lang.Object@1b6d3586". Subclasses override this to describe their value; having
    // it declared here is what lets `x.toString()` be called on any reference.
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    // Monitor signalling — the condition-variable half of `synchronized`. All native:
    // they drive the VM scheduler (suspend/wake threads on this object's monitor), out
    // of reach of bytecode. Must be called while holding this object's monitor.
    public final native void notify();

    public final native void notifyAll();

    public final native void wait();

    // Timed wait: park until notified or until `timeout` milliseconds elapse (`0` = wait
    // with no timeout, per the JLS). The VM measures the deadline on its opcode clock.
    public final native void wait(long timeout);
}
