package java.lang;

import java.io.PrintStream;

// KajiLibrary's java.lang.System. `out` is the standard output stream; a static
// initializer allocates the PrintStream (`System.out.println(x)` triggers this
// <clinit>, then calls the native println). The bulk-copy / identity-hash / gc
// operations are native — the VM does them directly.
public final class System {

    /**
     * The standard output stream. {@code final} because it is in the JDK (#203), and the
     * difference is not cosmetic: a public non-final field is one an unrelated class can
     * reassign, so nothing that reads {@code System.out} can rely on getting the same stream
     * twice.
     *
     * <p>It is a BLANK final -- declared here, assigned in the static initializer -- because the
     * PrintStream cannot be built until the class is initialising. That is legal (JLS 8.3.1.2:
     * a static final may be assigned exactly once in a static initialiser), and it is how the JDK
     * declares it too.
     */
    public static final PrintStream out;

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
    // ---- system properties ----
    //
    // WHICH properties exist is decided by the implementation, not by the specification: the JDK
    // documents a minimum set and every platform adds its own. So this answers the ones this VM
    // can answer truthfully -- the version, the separators, the operating system -- and `null`
    // for anything else, which is exactly what the contract says an absent key gives.

    /**
     * The value of the system property {@code key}, or null if there is none.
     *
     * @param key the property name
     * @throws IllegalArgumentException if {@code key} is empty
     * @throws NullPointerException if {@code key} is null
     */
    public static String getProperty(String key) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        if (key.length() == 0) {
            throw new IllegalArgumentException("key can't be empty");
        }
        return System.getProperty0(key);
    }

    /**
     * The value of the system property {@code key}, or {@code def} if there is none.
     *
     * @param key the property name
     * @param def what to answer when the property is absent
     * @throws IllegalArgumentException if {@code key} is empty
     * @throws NullPointerException if {@code key} is null
     */
    public static String getProperty(String key, String def) {
        String value = System.getProperty(key);
        if (value == null) {
            return def;
        }
        return value;
    }

    // The seam. Native because the values come from the VM and the platform it runs on, which is
    // not something bytecode can ask about.
    private static native String getProperty0(String key);

}
