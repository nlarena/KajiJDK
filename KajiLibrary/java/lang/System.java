package java.lang;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.Channel;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.function.Supplier;

// KajiLibrary's java.lang.System. The standard streams, the bulk-copy / identity-hash / clock
// operations (native, the VM does them directly), the system-property store, and the honest
// degradations of the process-level operations KajiJDK cannot perform.
//
// A KajiLibrary subset, all gated on classes this library does not model: {@code console()} (no
// {@code java.io.Console}), {@code get/setSecurityManager} (no {@code java.lang.SecurityManager};
// the security manager is degraded away in the reference too), and the two {@code getLogger}
// overloads (no {@code System.Logger} / {@code java.util.ResourceBundle}). Everything else is here.
public final class System {

    /**
     * The standard input stream. {@code final} because the JDK declares it so; reassigned only
     * through {@link #setIn(InputStream)}, which writes it via a native seam (a final field cannot
     * be reassigned by bytecode). KajiJDK has no real console input, so the default stream is at
     * end-of-stream from the start.
     */
    public static final InputStream in;

    /**
     * The standard output stream. See {@link #in} on why it is a blank {@code final} assigned in
     * the static initializer.
     */
    public static final PrintStream out;

    /**
     * The standard error stream. KajiJDK has a single console, so this writes to the same place as
     * {@link #out}; it is a distinct {@code PrintStream} object all the same, as the contract asks.
     */
    public static final PrintStream err;

    // The system-property store. Not final: setProperties can replace it wholesale. Seeded from the
    // VM's native property seam at class-init time.
    private static Properties props;

    // The line separator, cached at init as the JDK does (lineSeparator() never reflects a later
    // setProperty("line.separator", ...), matching the reference).
    private static String lineSeparator;

    // Non-instantiable: System is static-only (matches the JDK). Suppresses the
    // synthesized public default constructor.
    private System() {}

    static {
        // Autoflushing console streams. The underlying OutputStream is null: a console PrintStream
        // writes through the VM's text seam, not through a wrapped stream (there is no console file
        // descriptor to wrap). err is a distinct object writing to the same console, as specified.
        OutputStream sink = null;
        out = new PrintStream(sink, true);
        err = new PrintStream(sink, true);
        in = new NullInputStream();
        props = initProperties(new Properties());
        lineSeparator = props.getProperty("line.separator");
    }

    // The default System.in: a stream that is always at end-of-stream. KajiJDK reads no console
    // input, so `read()` returns -1 immediately rather than blocking on input that never comes.
    private static final class NullInputStream extends InputStream {
        public int read() {
            return -1;
        }
    }

    // ---- bulk / identity / time (native: the VM does these directly) ----

    public static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length);

    public static native int identityHashCode(Object x);

    // Milliseconds since the Unix epoch (1970-01-01T00:00:00Z) — the VM's wall clock. The one
    // seam java.time bottoms out at (Instant.now() / LocalDate.now()).
    public static native long currentTimeMillis();

    // A monotonic timer with an arbitrary origin, in nanoseconds. Never goes backwards; only
    // differences are meaningful. Backs scheduling delays.
    public static native long nanoTime();

    // The platform's file name for a native library (e.g. `foo` -> `foo.dll` / `libfoo.so`).
    public static native String mapLibraryName(String libname);

    // ---- garbage collection ----

    /**
     * Requests a garbage collection. The VM intercepts this and services the request at its next
     * safepoint (it never runs the collector inline), so this body is not reached.
     */
    public static void gc() {
        Runtime.getRuntime().gc();
    }

    /**
     * Deprecated and a no-op, as in the reference: there are no finalizers to run.
     *
     * @deprecated finalization is deprecated for removal.
     */
    @Deprecated
    public static void runFinalization() {
    }

    // ---- standard streams ----

    public static void setIn(InputStream in) {
        setIn0(in);
    }

    public static void setOut(PrintStream out) {
        setOut0(out);
    }

    public static void setErr(PrintStream err) {
        setErr0(err);
    }

    // The native seams behind the setters. Native because `in`/`out`/`err` are `final`, and only
    // native code may write a final field's slot (the reference JDK does exactly this).
    private static native void setIn0(InputStream in);

    private static native void setOut0(PrintStream out);

    private static native void setErr0(PrintStream err);

    // ---- system properties ----
    //
    // WHICH properties exist is decided by the implementation, not by the specification: the JDK
    // documents a minimum set and every platform adds its own. KajiJDK seeds the ones this VM can
    // answer truthfully -- the version, the separators, the operating system, the encodings -- into
    // a mutable store that get/set/clear then operate on.

    /**
     * The value of the system property {@code key}, or null if there is none.
     *
     * @param key the property name
     * @throws IllegalArgumentException if {@code key} is empty
     * @throws NullPointerException if {@code key} is null
     */
    public static String getProperty(String key) {
        checkKey(key);
        return props.getProperty(key);
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
        checkKey(key);
        return props.getProperty(key, def);
    }

    /**
     * Sets the system property {@code key} to {@code value}, returning the previous value or null.
     *
     * @throws IllegalArgumentException if {@code key} is empty
     * @throws NullPointerException if {@code key} or {@code value} is null
     */
    public static String setProperty(String key, String value) {
        checkKey(key);
        if (value == null) {
            throw new NullPointerException("value can't be null");
        }
        return (String) props.setProperty(key, value);
    }

    /**
     * Removes the system property {@code key}, returning its previous value or null.
     *
     * @throws IllegalArgumentException if {@code key} is empty
     * @throws NullPointerException if {@code key} is null
     */
    public static String clearProperty(String key) {
        checkKey(key);
        return (String) props.remove(key);
    }

    /** The whole system-property set (the live store, as the reference returns it). */
    public static Properties getProperties() {
        return props;
    }

    /**
     * Replaces the system properties. A null argument restores the VM's default set (re-seeded from
     * the platform), exactly as the reference specifies.
     */
    public static void setProperties(Properties p) {
        if (p == null) {
            props = initProperties(new Properties());
        } else {
            props = p;
        }
    }

    /** The system-dependent line separator (cached at init; never null). */
    public static String lineSeparator() {
        return lineSeparator;
    }

    // Seed a Properties with every key this VM's native seam can answer. Keys the seam returns
    // null for are simply absent, which is what an unset property means.
    private static Properties initProperties(Properties p) {
        String[] keys = {
            "java.version",
            "java.specification.version",
            "java.vm.specification.version",
            "java.vm.name",
            "java.vendor",
            "line.separator",
            "file.separator",
            "path.separator",
            "os.name",
            "os.arch",
            "native.encoding",
            "file.encoding",
        };
        int i = 0;
        while (i < keys.length) {
            String value = getProperty0(keys[i]);
            if (value != null) {
                p.setProperty(keys[i], value);
            }
            i = i + 1;
        }
        return p;
    }

    private static void checkKey(String key) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        if (key.length() == 0) {
            throw new IllegalArgumentException("key can't be empty");
        }
    }

    // The seam. Native because the values come from the VM and the platform it runs on, which is
    // not something bytecode can ask about.
    private static native String getProperty0(String key);

    // ---- environment ----
    //
    // KajiJDK exposes no process environment: it runs no subprocesses and inherits no meaningful
    // environment of its own, so the map is empty and every lookup misses. This is a truthful
    // answer (an absent variable is null / not in the map), not a stub that pretends.

    /** The value of the environment variable {@code name}, or null. Always null in KajiJDK. */
    public static String getenv(String name) {
        if (name == null) {
            throw new NullPointerException("name can't be null");
        }
        return null;
    }

    /** The (empty) process environment. */
    public static Map<String, String> getenv() {
        return new HashMap<String, String>();
    }

    // ---- process lifecycle / native libraries ----

    /**
     * Terminates the running VM with exit code {@code status}. The VM intercepts this call (it must
     * run shutdown and stop the interpreter), so this body is not reached.
     */
    public static void exit(int status) {
        Runtime.getRuntime().exit(status);
    }

    /** Loads a native library by file name. KajiJDK loads none, so this fails as the seam does. */
    public static void load(String filename) {
        Runtime.getRuntime().load(filename);
    }

    /** Loads a native library by library name. KajiJDK loads none, so this fails as the seam does. */
    public static void loadLibrary(String libname) {
        Runtime.getRuntime().loadLibrary(libname);
    }

    /**
     * The channel inherited from the entity that started this VM, or null when there is none.
     * KajiJDK is never started with an inherited channel, so this is always null.
     */
    public static Channel inheritedChannel() throws IOException {
        return null;
    }

    /**
     * The console for this VM, or null when there is none. KajiJDK has no controlling terminal, so
     * this is always null (see {@link Console}).
     */
    public static Console console() {
        return null;
    }

    // ---- security manager (degraded) ----
    //
    // The security manager is disabled in modern Java: there is never one installed, and installing
    // one is no longer permitted. KajiJDK mirrors that -- getSecurityManager is always null, and
    // setSecurityManager always fails.

    /** {@return null} — no security manager is ever installed. */
    @Deprecated
    public static SecurityManager getSecurityManager() {
        return null;
    }

    /**
     * @throws UnsupportedOperationException always -- a security manager cannot be set.
     */
    @Deprecated
    public static void setSecurityManager(SecurityManager sm) {
        throw new UnsupportedOperationException("Setting a SecurityManager is not supported");
    }

    // ---- platform logging (JEP 264) ----
    //
    // KajiJDK installs no LoggerFinder service (that abstraction, and its `getLogger(String,
    // Module)` seam, need `java.lang.Module`, which this library does not model), so getLogger
    // returns a simple built-in logger that writes to `System.err`. The public surface -- the
    // `Logger` interface and its `Level` enum -- is faithful; only the finder plumbing is absent.

    /** {@return a logger named {@code name}}. */
    public static Logger getLogger(String name) {
        if (name == null) {
            throw new NullPointerException("name can't be null");
        }
        return new DefaultLogger(name, null);
    }

    /** {@return a logger named {@code name} that localizes messages through {@code bundle}}. */
    public static Logger getLogger(String name, ResourceBundle bundle) {
        if (name == null) {
            throw new NullPointerException("name can't be null");
        }
        return new DefaultLogger(name, bundle);
    }

    /**
     * The platform logging interface (JEP 264): a minimal logging façade the JDK exposes so
     * libraries can log without depending on a concrete logging framework. A logger has a name,
     * a notion of which {@link Level}s it will act on, and a family of {@code log} methods that
     * all funnel into the two the implementor must define.
     */
    public interface Logger {

        /** The name of this logger. */
        String getName();

        /** Whether this logger acts on {@code level}. */
        boolean isLoggable(Level level);

        /** Logs a message at {@code level}. */
        default void log(Level level, String msg) {
            this.log(level, (ResourceBundle) null, msg, (Object[]) null);
        }

        /** Logs a lazily-produced message, computed only if the level is loggable. */
        default void log(Level level, Supplier<String> msgSupplier) {
            if (this.isLoggable(level)) {
                this.log(level, (ResourceBundle) null, msgSupplier.get(), (Object[]) null);
            }
        }

        /** Logs an object's string form (computed only if the level is loggable). */
        default void log(Level level, Object obj) {
            if (this.isLoggable(level)) {
                this.log(level, (ResourceBundle) null, obj.toString(), (Object[]) null);
            }
        }

        /** Logs a message and an associated throwable. */
        default void log(Level level, String msg, Throwable thrown) {
            this.log(level, null, msg, thrown);
        }

        /** Logs a lazily-produced message and a throwable. */
        default void log(Level level, Supplier<String> msgSupplier, Throwable thrown) {
            if (this.isLoggable(level)) {
                this.log(level, null, msgSupplier.get(), thrown);
            }
        }

        /** Logs a message with {@link java.text.MessageFormat}-style parameters. */
        default void log(Level level, String format, Object... params) {
            this.log(level, null, format, params);
        }

        /** Logs a localized message and a throwable. The core sink method. */
        void log(Level level, ResourceBundle bundle, String msg, Throwable thrown);

        /** Logs a localized, parameterized message. The other core sink method. */
        void log(Level level, ResourceBundle bundle, String format, Object... params);

        /**
         * The logging levels, from most verbose ({@link #ALL}) to none ({@link #OFF}). Each carries
         * a severity used to compare levels; the values match the reference exactly.
         */
        enum Level {

            ALL(Integer.MIN_VALUE),
            TRACE(400),
            DEBUG(500),
            INFO(800),
            WARNING(900),
            ERROR(1000),
            OFF(Integer.MAX_VALUE);

            private final int severity;

            private Level(int severity) {
                this.severity = severity;
            }

            /** The name of this level (its enum constant name). */
            public final String getName() {
                return this.name();
            }

            /** This level's severity, for ordering levels against a threshold. */
            public final int getSeverity() {
                return this.severity;
            }
        }
    }

    // The built-in Logger: writes to System.err, localizing through a bundle when one is supplied
    // (either the logger's own, from getLogger(name, bundle), or one passed to a log call).
    private static final class DefaultLogger implements Logger {

        private final String name;
        private final ResourceBundle bundle;

        private DefaultLogger(String name, ResourceBundle bundle) {
            this.name = name;
            this.bundle = bundle;
        }

        public String getName() {
            return this.name;
        }

        public boolean isLoggable(Logger.Level level) {
            return level != Logger.Level.OFF;
        }

        public void log(Logger.Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            if (msg == null || !this.isLoggable(level)) {
                return;
            }
            System.err.println("[" + level.getName() + "] " + this.name + ": "
                    + this.localize(bundle, msg));
            if (thrown != null) {
                thrown.printStackTrace();
            }
        }

        public void log(Logger.Level level, ResourceBundle bundle, String format, Object... params) {
            if (format == null || !this.isLoggable(level)) {
                return;
            }
            // KajiJDK's built-in logger does not expand MessageFormat placeholders; it prints the
            // (localized) pattern as-is, which is a truthful, if unformatted, record.
            System.err.println("[" + level.getName() + "] " + this.name + ": "
                    + this.localize(bundle, format));
        }

        // Localize `key` through `override` if given, else this logger's own bundle, else pass it
        // through. A lookup miss is not fatal for logging, so it falls back to the raw text.
        private String localize(ResourceBundle override, String key) {
            ResourceBundle b = (override != null) ? override : this.bundle;
            if (b == null) {
                return key;
            }
            try {
                return b.getString(key);
            } catch (RuntimeException e) {
                return key;
            }
        }
    }
}
