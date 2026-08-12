package java.lang;

// Minimal java.lang.System. `out` is the standard output stream; the real VM wires
// it up natively, here a static initializer just allocates our PrintStream. So
// `System.out.println(n)` resolves out (triggering System.<clinit>), then calls the
// native println.
public class System {
    public static java.io.PrintStream out;

    static {
        out = new java.io.PrintStream();
    }

    // Bulk array copy and identity hash — both native (the VM does them directly).
    public static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length);

    public static native int identityHashCode(Object x);

    // Monotonic timer in nanoseconds with an arbitrary origin (per the spec — good only for
    // measuring *elapsed* time, e.g. scheduling delays). The VM reads a real clock.
    public static native long nanoTime();

    // Requests a garbage collection. The VM intercepts this call and services the
    // request at its next safepoint (it never runs the collector inline).
    public static native void gc();
}
