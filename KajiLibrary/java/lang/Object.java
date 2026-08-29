package java.lang;

/**
 * KajiLibrary's java.lang.Object — the root of every class, written from scratch for
 * KajiJDK (not inherited from bootstrap/). Methods the VM must implement are `native`
 * — getClass reads the object header, hashCode and the monitor ops touch the VM — and
 * everything expressible in plain Java is plain Java.
 */
public class Object {

    public Object() {
    }

    /**
     * The runtime class of this object: the VM reads it from the object header (its
     * class id), which bytecode cannot do. Pure intrinsic.
     */
    public final native Class<?> getClass();

    /**
     * Identity hash code — the VM's notion of the object's identity, which Java cannot
     * compute itself (we use the heap offset).
     */
    public native int hashCode();

    /**
     * Reference equality by default (§java.lang.Object): two references are equal only
     * if they denote the same object. Subclasses override this for value equality.
     */
    public boolean equals(Object obj) {
        return this == obj;
    }

    /**
     * The root string representation: the class name and the identity hash in hex, e.g.
     * "java.lang.Object@1b6d3586". Subclasses override this to describe their value; having
     * it declared here is what lets `x.toString()` be called on any reference.
     */
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * Monitor signalling — the condition-variable half of `synchronized`. All native:
     * they drive the VM scheduler (suspend/wake threads on this object's monitor), out
     * of reach of bytecode. Must be called while holding this object's monitor.
     */
    public final native void notify();

    public final native void notifyAll();

    /**
     * Waits until notified. Plain Java over the timed form, as in the JDK: one seam into the
     * scheduler is enough, and the untimed wait is the timed one with no deadline.
     */
    public final void wait() throws InterruptedException {
        this.wait(0L);
    }

    /**
     * Timed wait: park until notified or until `timeout` milliseconds elapse (`0` = wait
     * with no timeout, per the JLS). The VM measures the deadline on its opcode clock.
     */
    public final void wait(long timeout) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }
        this.wait0(timeout);
    }

    // La costura al scheduler, privada y con el mismo nombre que le pone el JDK. Que el metodo
    // publico NO sea nativo no es cosmetico: es lo unico que lo hace igual al de referencia
    // (`Method.getModifiers()` responde distinto), y ademas es donde entra la validacion del
    // argumento, que un nativo tendria que duplicar.
    private final native void wait0(long timeout) throws InterruptedException;

    /**
     * The nanosecond form, which does not actually offer nanosecond resolution and never
     * claimed to: it ROUNDS UP to the next millisecond, so that a caller asking for a sliver
     * of time is not told to wait forever by a truncation to zero.
     */
    public final void wait(long timeout, int nanos) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout cannot be negative");
        }
        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException("nanosecond timeout out of range");
        }
        long millis = timeout;
        if (nanos > 0 && millis < 9223372036854775807L) {
            millis = millis + 1L;
        }
        this.wait(millis);
    }

    /**
     * A shallow copy, field for field, without running a constructor -- which is why it has to
     * be the VM's job. It refuses unless the class opted in by implementing Cloneable: copying
     * an object whose author never considered it is how aliasing bugs are made.
     */
    protected native Object clone() throws CloneNotSupportedException;

    /**
     * Called before the object is reclaimed. Empty here and deprecated everywhere: finalization
     * gives no guarantee about WHEN it runs, or that it runs at all, so anything that must
     * happen belongs in a close() the caller can see.
     */
    @Deprecated(since = "9", forRemoval = true)
    protected void finalize() throws Throwable {
    }
}
