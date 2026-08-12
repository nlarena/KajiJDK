package java.lang;

import java.io.PrintStream;

// KajiLibrary's java.lang.System. `out` is the standard output stream; a static
// initializer allocates the PrintStream (`System.out.println(x)` triggers this
// <clinit>, then calls the native println). The bulk-copy / identity-hash / gc
// operations are native — the VM does them directly.
public final class System {

    public static PrintStream out;

    // Non-instantiable: System is static-only (matches the JDK). Suppresses the
    // synthesized public default constructor.
    private System() {}

    static {
        out = new PrintStream();
    }

    public static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length);

    public static native int identityHashCode(Object x);

    // Milliseconds since the Unix epoch (1970-01-01T00:00:00Z) — the VM's wall clock. The one
    // seam java.time bottoms out at (Instant.now() / LocalDate.now()).
    public static native long currentTimeMillis();

    // Requests a garbage collection. The VM intercepts this and services the request at
    // its next safepoint (it never runs the collector inline).
    public static native void gc();
}
