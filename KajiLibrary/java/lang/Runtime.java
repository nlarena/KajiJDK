package java.lang;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// KajiLibrary's java.lang.Runtime — the interaction with the runtime environment. KajiJDK cannot
// spawn subprocesses, load native libraries, or terminate its host, so those methods degrade
// honestly (an `UnsupportedOperationException`, an `UnsatisfiedLinkError`, a no-op) rather than
// pretending. What works: the singleton, {@link Version}, {@code availableProcessors}, the memory
// figures (fixed but plausible), a GC hint, and shutdown-hook bookkeeping.
public class Runtime {

    private static final Runtime CURRENT = new Runtime();

    // Registered shutdown hooks, in registration order. Never run here -- there is no host process
    // for KajiJDK to terminate -- but tracked so add/remove behave.
    private final List<Thread> hooks = new ArrayList<Thread>();

    private Runtime() {
    }

    /** The single {@code Runtime} of the current process. */
    public static Runtime getRuntime() {
        return CURRENT;
    }

    /** The version of this runtime. */
    public static Version version() {
        return Version.parse("25.0.3+9");
    }

    /** How many processors the VM can use (a lower bound of 1). */
    public native int availableProcessors();

    // ---- memory ----

    /** A hint to run the garbage collector. KajiJDK's GC runs on its own schedule; this is a no-op. */
    public native void gc();

    public native long freeMemory();

    public native long totalMemory();

    public native long maxMemory();

    /** Deprecated and a no-op, as in the reference. */
    public void runFinalization() {
    }

    // ---- shutdown ----

    public void addShutdownHook(Thread hook) {
        if (hook == null) {
            throw new NullPointerException();
        }
        this.hooks.add(hook);
    }

    public boolean removeShutdownHook(Thread hook) {
        return this.hooks.remove(hook);
    }

    /** Terminate the JVM. KajiJDK has no host process to terminate, so this does not return control. */
    public void exit(int status) {
        // No process to end; a real terminate would run the hooks then halt.
    }

    public void halt(int status) {
        // No process to end.
    }

    // ---- subprocesses / native libraries (not supported) ----

    public Process exec(String command) {
        throw new UnsupportedOperationException("KajiJDK cannot spawn subprocesses");
    }

    public Process exec(String[] cmdarray) {
        throw new UnsupportedOperationException("KajiJDK cannot spawn subprocesses");
    }

    public Process exec(String command, String[] envp) {
        throw new UnsupportedOperationException("KajiJDK cannot spawn subprocesses");
    }

    public Process exec(String[] cmdarray, String[] envp) {
        throw new UnsupportedOperationException("KajiJDK cannot spawn subprocesses");
    }

    public Process exec(String command, String[] envp, File dir) {
        throw new UnsupportedOperationException("KajiJDK cannot spawn subprocesses");
    }

    public Process exec(String[] cmdarray, String[] envp, File dir) {
        throw new UnsupportedOperationException("KajiJDK cannot spawn subprocesses");
    }

    public void load(String filename) {
        throw new UnsatisfiedLinkError("KajiJDK loads no native libraries");
    }

    public void loadLibrary(String libname) {
        throw new UnsatisfiedLinkError("KajiJDK loads no native libraries");
    }

    // ---- JEP 223 version ----

    /**
     * A representation of a version string ({@code $FEATURE.$INTERIM.$UPDATE.$PATCH-$PRE+$BUILD-$OPT}).
     * Immutable, and ordered by version number, then pre-release, then build, then optional.
     */
    public static final class Version implements Comparable<Version> {

        private final List<Integer> version;
        private final Optional<String> pre;
        private final Optional<Integer> build;
        private final Optional<String> optional;

        private Version(List<Integer> version, Optional<String> pre, Optional<Integer> build,
                Optional<String> optional) {
            this.version = version;
            this.pre = pre;
            this.build = build;
            this.optional = optional;
        }

        /**
         * Parses {@code s} into a {@code Version}.
         *
         * @throws NullPointerException if {@code s} is null
         * @throws IllegalArgumentException if {@code s} is not a valid version string
         */
        public static Version parse(String s) {
            if (s == null) {
                throw new NullPointerException();
            }
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Empty version string");
            }
            // Split off the leading version number (digits and dots) from the rest.
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || c == '.') {
                    i = i + 1;
                } else {
                    break;
                }
            }
            String vnum = s.substring(0, i);
            String rest = s.substring(i);
            List<Integer> version = parseVnum(vnum);

            Optional<String> pre = Optional.empty();
            Optional<Integer> build = Optional.empty();
            Optional<String> optional = Optional.empty();
            if (!rest.isEmpty()) {
                char first = rest.charAt(0);
                int plus = rest.indexOf('+');
                if (first == '-') {
                    if (plus >= 0) {
                        pre = Optional.of(rest.substring(1, plus));
                        String tail = rest.substring(plus + 1);
                        int dash = tail.indexOf('-');
                        if (dash >= 0) {
                            build = Optional.of(Integer.valueOf(Integer.parseInt(tail.substring(0, dash))));
                            optional = Optional.of(tail.substring(dash + 1));
                        } else if (!tail.isEmpty()) {
                            build = Optional.of(Integer.valueOf(Integer.parseInt(tail)));
                        }
                    } else {
                        pre = Optional.of(rest.substring(1));
                    }
                } else if (first == '+') {
                    String tail = rest.substring(1);
                    int dash = tail.indexOf('-');
                    if (dash >= 0) {
                        build = Optional.of(Integer.valueOf(Integer.parseInt(tail.substring(0, dash))));
                        optional = Optional.of(tail.substring(dash + 1));
                    } else if (!tail.isEmpty()) {
                        build = Optional.of(Integer.valueOf(Integer.parseInt(tail)));
                    }
                } else {
                    throw new IllegalArgumentException("Invalid version string: " + s);
                }
            }
            return new Version(version, pre, build, optional);
        }

        private static List<Integer> parseVnum(String vnum) {
            List<Integer> out = new ArrayList<Integer>();
            int start = 0;
            int i = 0;
            while (i <= vnum.length()) {
                if (i == vnum.length() || vnum.charAt(i) == '.') {
                    String part = vnum.substring(start, i);
                    if (part.isEmpty()) {
                        throw new IllegalArgumentException("Invalid version number: " + vnum);
                    }
                    out.add(Integer.valueOf(Integer.parseInt(part)));
                    start = i + 1;
                }
                i = i + 1;
            }
            return out;
        }

        public int feature() {
            return this.version.get(0).intValue();
        }

        public int interim() {
            return this.version.size() > 1 ? this.version.get(1).intValue() : 0;
        }

        public int update() {
            return this.version.size() > 2 ? this.version.get(2).intValue() : 0;
        }

        public int patch() {
            return this.version.size() > 3 ? this.version.get(3).intValue() : 0;
        }

        public int major() {
            return this.feature();
        }

        public int minor() {
            return this.interim();
        }

        public int security() {
            return this.update();
        }

        public List<Integer> version() {
            return this.version;
        }

        public Optional<String> pre() {
            return this.pre;
        }

        public Optional<Integer> build() {
            return this.build;
        }

        public Optional<String> optional() {
            return this.optional;
        }

        public int compareTo(Version other) {
            return this.compare(other, false);
        }

        public int compareToIgnoreOptional(Version other) {
            return this.compare(other, true);
        }

        private int compare(Version other, boolean ignoreOptional) {
            int c = compareVersion(other);
            if (c != 0) {
                return c;
            }
            c = comparePre(other);
            if (c != 0) {
                return c;
            }
            c = compareBuild(other);
            if (c != 0 || ignoreOptional) {
                return c;
            }
            return compareOptional(other);
        }

        private int compareVersion(Version other) {
            int n = Math.max(this.version.size(), other.version.size());
            int i = 0;
            while (i < n) {
                int a = i < this.version.size() ? this.version.get(i).intValue() : 0;
                int b = i < other.version.size() ? other.version.get(i).intValue() : 0;
                if (a != b) {
                    return a < b ? -1 : 1;
                }
                i = i + 1;
            }
            return 0;
        }

        private int comparePre(Version other) {
            // A version WITH a pre-release precedes the otherwise-equal version WITHOUT one.
            if (!this.pre.isPresent() && !other.pre.isPresent()) {
                return 0;
            }
            if (!this.pre.isPresent()) {
                return 1;
            }
            if (!other.pre.isPresent()) {
                return -1;
            }
            String p = this.pre.get();
            String q = other.pre.get();
            boolean pNum = isNumeric(p);
            boolean qNum = isNumeric(q);
            if (pNum && qNum) {
                int a = Integer.parseInt(p);
                int b = Integer.parseInt(q);
                return a < b ? -1 : (a > b ? 1 : 0);
            }
            if (pNum) {
                return -1;
            }
            if (qNum) {
                return 1;
            }
            return p.compareTo(q);
        }

        private int compareBuild(Version other) {
            if (!this.build.isPresent() && !other.build.isPresent()) {
                return 0;
            }
            if (!this.build.isPresent()) {
                return -1;
            }
            if (!other.build.isPresent()) {
                return 1;
            }
            int a = this.build.get().intValue();
            int b = other.build.get().intValue();
            return a < b ? -1 : (a > b ? 1 : 0);
        }

        private int compareOptional(Version other) {
            if (!this.optional.isPresent() && !other.optional.isPresent()) {
                return 0;
            }
            if (!this.optional.isPresent()) {
                return -1;
            }
            if (!other.optional.isPresent()) {
                return 1;
            }
            return this.optional.get().compareTo(other.optional.get());
        }

        private static boolean isNumeric(String s) {
            if (s.isEmpty()) {
                return false;
            }
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Version)) {
                return false;
            }
            Version that = (Version) other;
            return this.compare(that, false) == 0
                    && this.optional.equals(that.optional);
        }

        public boolean equalsIgnoreOptional(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Version)) {
                return false;
            }
            return this.compareToIgnoreOptional((Version) other) == 0;
        }

        public int hashCode() {
            int h = this.version.hashCode();
            h = 31 * h + this.pre.hashCode();
            h = 31 * h + this.build.hashCode();
            h = 31 * h + this.optional.hashCode();
            return h;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < this.version.size()) {
                if (i > 0) {
                    sb.append('.');
                }
                sb.append(this.version.get(i));
                i = i + 1;
            }
            if (this.pre.isPresent()) {
                sb.append('-').append(this.pre.get());
            }
            if (this.build.isPresent()) {
                sb.append('+').append(this.build.get());
                if (this.optional.isPresent()) {
                    sb.append('-').append(this.optional.get());
                }
            } else if (this.optional.isPresent()) {
                sb.append(this.pre.isPresent() ? '+' : '-').append(this.optional.get());
            }
            return sb.toString();
        }
    }
}
