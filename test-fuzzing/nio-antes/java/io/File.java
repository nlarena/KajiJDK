package java.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import jdk.internal.io.Fs;

// KajiLibrary's java.io.File -- an abstract path name.
//
// The half that manipulates paths is complete (names, parents, absolutization, normalization,
// conversion to URI/URL, ordering). The other half --the one that touches the disk-- rests on
// `jdk.internal.io.Fs`, and today it really answers: existence and permissions (`stat`), size
// (`size`), date (`mtime`/`setMtime`), listing (`list`), canonical path (`canonical`), creation and
// deletion.
//
// **What is still inert, and why.** Four groups of methods return `false`, `0` or empty because the
// native that would answer them does not exist. They do not invent an answer: they say they could
// not, which is what the contract allows them to say.
//
//   - `renameTo` -- renaming needs a native of its own. Simulating it with copy-and-delete would
//     not be a rename: it is not atomic, it does not work over directories, and it loses the
//     metadata.
//   - `setReadOnly` / `setWritable` / `setReadable` / `setExecutable` -- changing permissions.
//     Returning `true` without having changed them would turn an "I could not" into a "done".
//   - `getTotalSpace` / `getFreeSpace` / `getUsableSpace` -- `0L` is what the contract orders to be
//     returned when the partition cannot be queried.
//   - `listRoots` -- an empty array, which the contract explicitly admits.
//
// `isHidden` looks at the name's leading dot, which is Unix's rule. On Windows being hidden is an
// attribute of the file and not a naming convention, so there the answer may differ from the JDK's;
// it is left because it is the only rule that can be applied with no native, and erring towards "it
// is not hidden" breaks nothing that depends on this.
//
public class File implements Serializable, Comparable<File> {

    /** The system-dependent name-separator character. */
    public static final char separatorChar = System.getProperty("file.separator").charAt(0);

    /** The system-dependent name-separator, as a string. */
    public static final String separator = String.valueOf(separatorChar);

    /** The system-dependent path-separator character. */
    public static final char pathSeparatorChar = System.getProperty("path.separator").charAt(0);

    /** The system-dependent path-separator, as a string. */
    public static final String pathSeparator = String.valueOf(pathSeparatorChar);

    // The normalized abstract path name.
    private final String path;

    public File(String pathname) {
        this.path = normalize(pathname);
    }

    public File(String parent, String child) {
        if (child == null) {
            throw new NullPointerException("child cannot be null");
        }
        if (parent == null || parent.length() == 0) {
            this.path = normalize(child);
        } else {
            this.path = normalize(parent + separatorChar + child);
        }
    }

    public File(File parent, String child) {
        this(parent == null ? null : parent.path, child);
    }

    public File(URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri cannot be null");
        }
        String p = uri.getPath();
        if (p == null) {
            throw new IllegalArgumentException("URI has no path: " + uri);
        }
        this.path = normalize(p);
    }

    // ---- path manipulation ----

    /** The name of the file or directory this path denotes (its last segment). */
    public String getName() {
        int i = this.path.lastIndexOf(separatorChar);
        return (i < 0) ? this.path : this.path.substring(i + 1);
    }

    /** The parent path, or null if this path names no parent. */
    public String getParent() {
        int i = this.path.lastIndexOf(separatorChar);
        if (i < 0) {
            return null;
        }
        if (i == 0) {
            return separator; // parent of "/x" is "/"
        }
        return this.path.substring(0, i);
    }

    /** The parent as a {@code File}, or null. */
    public File getParentFile() {
        String p = this.getParent();
        return (p == null) ? null : new File(p);
    }

    /** This path in string form. */
    public String getPath() {
        return this.path;
    }

    /** Whether this path is absolute. */
    public boolean isAbsolute() {
        if (this.path.length() == 0) {
            return false;
        }
        if (separatorChar == '\\') {
            // Windows: a drive-letter root ("C:\") or a UNC path ("\\host").
            if (this.path.length() >= 3 && this.path.charAt(1) == ':'
                    && this.path.charAt(2) == separatorChar) {
                return true;
            }
            return this.path.length() >= 2 && this.path.charAt(0) == separatorChar
                    && this.path.charAt(1) == separatorChar;
        }
        // POSIX: a leading '/'.
        return this.path.charAt(0) == separatorChar;
    }

    /** This path made absolute (relative paths are resolved against the working directory). */
    public String getAbsolutePath() {
        if (this.isAbsolute()) {
            return this.path;
        }
        String cwd = System.getProperty("user.dir");
        if (cwd == null || cwd.length() == 0) {
            cwd = separator;
        }
        return normalize(cwd + separatorChar + this.path);
    }

    /** This path made absolute, as a {@code File}. */
    public File getAbsoluteFile() {
        return new File(this.getAbsolutePath());
    }

    /**
     * The **canonical** path: absolute, with no `.` and no `..`, with the links resolved and --on
     * Windows-- with the capitalization the disk really has.
     *
     * <p>It is asked of the system (`Fs.canonical`) and not worked out over the string, because it
     * is the only way for two different paths naming the same file to give the same result: on
     * Windows `C:\A.TXT` and `c:\a.txt` are the same file, and no text manipulation knows that.
     *
     * <p><strong>A file that does not exist has a canonical path all the same.</strong> The
     * contract asks for it --canonicalizing is an operation on the name-- and the native cannot
     * give it, because canonicalizing what is not there has no answer from the system. So for those
     * the nearest ancestor that **does** exist is canonicalized and the missing names are hung back
     * onto it. The result has the real capitalization as far as the disk could tell it and the
     * written one from there on, which is exactly what the JDK does.
     *
     * @throws IOException if not even the absolute path could be built
     */
    public String getCanonicalPath() throws IOException {
        String abs = normalizeDots(this.getAbsolutePath());
        String direct = stripVerbatim(Fs.canonical(abs));
        if (direct != null) {
            return direct;
        }
        // It does not exist: it climbs to the first ancestor that does, and rebuilds from there.
        StringBuilder tail = new StringBuilder();
        String current = abs;
        while (true) {
            int cut = current.lastIndexOf(separatorChar);
            if (cut < 0) {
                return abs;                       // no parent to ask: the absolute one and done
            }
            String name = current.substring(cut + 1);
            current = cut == 0 ? separator : current.substring(0, cut);
            if (name.length() != 0) {
                tail.insert(0, name);
                tail.insert(0, separatorChar);
            }
            String base = stripVerbatim(Fs.canonical(current));
            if (base != null) {
                // A root already ends in a separator (`C:\`); sticking another on would give
                // `C:\\x`.
                if (base.length() > 0 && base.charAt(base.length() - 1) == separatorChar) {
                    return base + tail.substring(1);
                }
                return base + tail;
            }
            if (current.equals(separator) || current.length() == 0) {
                return abs;
            }
        }
    }

    /** The canonical path, as a {@code File}. */
    public File getCanonicalFile() throws IOException {
        return new File(this.getCanonicalPath());
    }

    /**
     * It takes off a Windows path the "verbatim" prefix (`\\?\`) it comes back from the system
     * with.
     *
     * <p>The native returns the extended form because it is the one the system uses inside; the JDK
     * returns `C:\x` and not `\\?\C:\x`, and whoever compares `getCanonicalPath()`'s result with a
     * hand-written path expects the short one. `\\?\UNC\server\share` goes back to being
     * `\\server\share`, which is its normal form.
     */
    private static String stripVerbatim(String p) {
        if (p == null) {
            return null;
        }
        if (p.startsWith("\\\\?\\UNC\\")) {
            return "\\\\" + p.substring(8);
        }
        if (p.startsWith("\\\\?\\")) {
            return p.substring(4);
        }
        return p;
    }

    /**
     * It resolves `.` and `..` over the text of an already absolute path.
     *
     * <p>It is what gets handed to the native. Doing it beforehand matters because `..` is resolved
     * **over the names** and not over the links: if the native fails --the file does not exist--
     * the path left for the fallback route is already clean.
     */
    private static String normalizeDots(String p) {
        // It is split by hand and not with `split`: `split` is a regular expression, and dragging
        // `java.util.regex` into `java.io.File` --which is among the first classes loaded-- for the
        // sake of splitting on two characters would be paying half a library for an `indexOf`.
        String[] parts = new String[countParts(p)];
        int howMany = 0;
        int from = 0;
        for (int k = 0; k <= p.length(); k++) {
            if (k == p.length() || p.charAt(k) == '\\' || p.charAt(k) == '/') {
                parts[howMany] = p.substring(from, k);
                howMany = howMany + 1;
                from = k + 1;
            }
        }
        String[] stack = new String[parts.length];
        int n = 0;
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i];
            if (s.equals(".") || (s.length() == 0 && i != 0)) {
                continue;
            }
            // A `..` at the start of an absolute path has nowhere to climb to: it is discarded,
            // which is what the system does with `C:\..`.
            if (s.equals("..")) {
                if (n > 1) {
                    n = n - 1;
                }
                continue;
            }
            stack[n] = s;
            n = n + 1;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(separatorChar);
            }
            sb.append(stack[i]);
        }
        String r = sb.toString();
        return r.length() == 0 ? separator : r;
    }

    /** How many stretches splitting on separators leaves. It is counted beforehand so as to size
     * the array. */
    private static int countParts(String p) {
        int n = 1;
        for (int i = 0; i < p.length(); i++) {
            if (p.charAt(i) == '\\' || p.charAt(i) == '/') {
                n = n + 1;
            }
        }
        return n;
    }

    // ---- URI / URL ----

    /** A {@code file:} URI for this abstract path. */
    public URI toURI() {
        // The path is handed RAW to `URI`'s multi-part constructor, and it is that one that escapes
        // it.
        //
        // Building the text by hand and passing it through `URI.create` --which is what it used to
        // do-- cannot work: the single-`String` constructor expects an **already escaped** URI, so
        // a file name with a space produced `file:/a b/c`, which is not a valid URI, and
        // `getRawPath()` returned the space unencoded. The multi-part constructor exists precisely
        // for this.
        try {
            return new URI("file", null, withSlashes(this.getAbsolutePath(), this.isDirectory()), null);
        } catch (URISyntaxException impossible) {
            // An already escaped absolute path is always a valid URI; if it were not, that would be
            // a defect of this class and not something the caller could handle.
            throw new Error(impossible);
        }
    }

    // The path with the URI's slashes: this system's separator turned into '/', a leading slash if
    // missing, and a trailing slash for a directory -- that last one is what makes `resolve`
    // against a directory's URI add to the directory instead of replacing its last segment.
    private static String withSlashes(String p, boolean directory) {
        StringBuilder sb = new StringBuilder();
        if (p.length() == 0 || p.charAt(0) != separatorChar) {
            sb.append('/');
        }
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            sb.append(c == separatorChar ? '/' : c);
            i = i + 1;
        }
        if (directory && (sb.length() == 0 || sb.charAt(sb.length() - 1) != '/')) {
            sb.append('/');
        }
        return sb.toString();
    }

    /**
     * @deprecated use {@link #toURI()} then {@link URI#toString()} with a URL when one is needed.
     */
    @Deprecated
    public URL toURL() throws MalformedURLException {
        return new URL(this.toURI().toString());
    }

    // ---- the state on the disk ----
    //
    // All six come out of **one single** call to `Fs.stat`, which returns the flags together. It is
    // on purpose: asking for them separately would touch the disk once per flag and --worse-- could
    // give answers from different moments if something changed in between. Here each method does
    // its own query all the same, because a `File` caches nothing: the file may appear or disappear
    // between two calls, and a stored answer would be a lie with a date on it.

    public boolean canRead() {
        return (Fs.stat(this.path) & Fs.CAN_READ) != 0;
    }

    public boolean canWrite() {
        return (Fs.stat(this.path) & Fs.CAN_WRITE) != 0;
    }

    /**
     * Whether it can be executed.
     *
     * <p>It is answered with "it can be read", which on Windows is the same thing for any file and
     * on POSIX is not. It is the only one of the three this library does not tell apart, and it is
     * documented instead of returning `false` --which would be lying about every executable-- or
     * `true` --which would be lying about everything else.
     */
    public boolean canExecute() {
        return (Fs.stat(this.path) & Fs.CAN_READ) != 0;
    }

    public boolean exists() {
        return (Fs.stat(this.path) & Fs.EXISTS) != 0;
    }

    public boolean isDirectory() {
        return (Fs.stat(this.path) & Fs.IS_DIRECTORY) != 0;
    }

    public boolean isFile() {
        return (Fs.stat(this.path) & Fs.IS_FILE) != 0;
    }

    public boolean isHidden() {
        return this.getName().startsWith(".");
    }

    /**
     * When it was last modified, in milliseconds since the epoch; `0L` if it could not be known.
     *
     * <p>The contract's zero is ambiguous on purpose and one has to know it: it means "it does not
     * exist or the query failed", **and it is also** the valid date of the 1st of January 1970.
     * That is why the native does not use zero as a sentinel but `Long.MIN_VALUE`, and the
     * translation to zero is done here, in the only place the contract forces it. Whoever needs to
     * tell the two cases apart has {@link #exists()}.
     */
    public long lastModified() {
        long t = Fs.mtime(this.path);
        return t == Long.MIN_VALUE ? 0L : t;
    }

    public long length() {
        return Fs.size(this.path);
    }

    // ---- mutation ----

    /**
     * It creates the file if it does not exist. `true` if this call created it.
     *
     * <p>The check and the creation are **not** atomic here, unlike the JDK's: between the
     * `exists()` and the `writeAllBytes` another process may create the file, and then this returns
     * `true` having overwritten it with emptiness. It is said outright because the JDK's javadoc
     * promises atomicity and this one does not have it.
     */
    public boolean createNewFile() throws IOException {
        if (this.exists()) {
            return false;
        }
        return Fs.writeAllBytes(this.path, new byte[0], false);
    }

    public boolean delete() {
        return Fs.delete(this.path);
    }

    public void deleteOnExit() {
    }

    /**
     * The **simple** names of what is in this directory, or `null`.
     *
     * <p>`null` is no accidental error case: the contract says it is returned when this is not a
     * directory or there was an I/O failure, and it is what tells "I could not look" from "I looked
     * and it is empty" --which is a zero-length array. Losing that distinction would turn an error
     * into a result, and whoever walks the tree would never hear that a branch was missing.
     *
     * <p>The names are simple, without this directory's path in front. {@link #listFiles()} is the
     * variant that adds it.
     *
     * <p>The order is the one the file system gives: the contract **guarantees none**, and sorting
     * it here would make somebody lean on one that another platform is not going to give them.
     */
    public String[] list() {
        return Fs.list(this.path);
    }

    /** The same, keeping only the names the filter accepts. */
    public String[] list(FilenameFilter filter) {
        String[] all = this.list();
        if (all == null) {
            return null;
        }
        if (filter == null) {
            return all;
        }
        // It counts and then copies, instead of using a list: it is the same walk twice in exchange
        // for not depending on `java.util` from `java.io`, which is loaded earlier.
        int n = 0;
        for (String name : all) {
            if (filter.accept(this, name)) {
                n = n + 1;
            }
        }
        String[] out = new String[n];
        int k = 0;
        for (String name : all) {
            if (filter.accept(this, name)) {
                out[k] = name;
                k = k + 1;
            }
        }
        return out;
    }

    /**
     * What is in this directory, as `File`s, or `null`.
     *
     * <p>Each one is built with **this** file as its parent, so the paths come out complete and
     * relative or absolute according to what this one is. It is the difference from {@link
     * #list()}, which gives bare names.
     */
    public File[] listFiles() {
        String[] names = this.list();
        if (names == null) {
            return null;
        }
        File[] out = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = new File(this, names[i]);
        }
        return out;
    }

    /**
     * The same, filtering by name.
     *
     * <p>The filter receives the **directory and the name**, not the built `File`: it is the
     * difference from {@link #listFiles(FileFilter)}, and it serves to filter without constructing
     * an object per entry.
     */
    public File[] listFiles(FilenameFilter filter) {
        String[] names = this.list(filter);
        if (names == null) {
            return null;
        }
        File[] out = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = new File(this, names[i]);
        }
        return out;
    }

    /** The same, filtering by the already built `File` --which is what allows asking it whether it
     * is a directory. */
    public File[] listFiles(FileFilter filter) {
        File[] all = this.listFiles();
        if (all == null || filter == null) {
            return all;
        }
        int n = 0;
        for (File f : all) {
            if (filter.accept(f)) {
                n = n + 1;
            }
        }
        File[] out = new File[n];
        int k = 0;
        for (File f : all) {
            if (filter.accept(f)) {
                out[k] = f;
                k = k + 1;
            }
        }
        return out;
    }

    public boolean mkdir() {
        return Fs.mkdir(this.path, false);
    }

    /** The same, also creating whatever parent directories are missing. */
    public boolean mkdirs() {
        return Fs.mkdir(this.path, true);
    }

    public boolean renameTo(File dest) {
        return false;
    }

    /**
     * It sets the last-modification date.
     *
     * @throws IllegalArgumentException if `time` is negative -- the contract asks for it, and it is
     *     different from returning `false`: a `false` says "it could not be done", this says "that
     *     cannot be asked for"
     */
    public boolean setLastModified(long time) {
        if (time < 0L) {
            throw new IllegalArgumentException("Negative time");
        }
        return Fs.setMtime(this.path, time);
    }

    public boolean setReadOnly() {
        return false;
    }

    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }

    public boolean setWritable(boolean writable) {
        return false;
    }

    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return false;
    }

    public boolean setReadable(boolean readable) {
        return false;
    }

    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return false;
    }

    public boolean setExecutable(boolean executable) {
        return false;
    }

    // ---- roots / space / temp (inert) ----

    /** The file-system roots. KajiJDK exposes none. */
    public static File[] listRoots() {
        return new File[0];
    }

    public long getTotalSpace() {
        return 0L;
    }

    public long getFreeSpace() {
        return 0L;
    }

    public long getUsableSpace() {
        return 0L;
    }

    // A counter of temporary names. It starts at the clock so that two consecutive runs of the same
    // program do not start from the same name and overwrite each other.
    private static long tempSeed = System.nanoTime();

    /**
     * It creates an **empty** temporary file in `directory` (or in `java.io.tmpdir` if that is
     * null) and returns the {@code File} that names it.
     *
     * <p><strong>It is not atomic, and in the JDK it is.</strong> The JDK creates the file with the
     * system's "fail if it exists" flag, one single operation; here it asks whether it exists and
     * then creates it, in two. Between the two, another process may create that same name and this
     * call would overwrite it with emptiness. The window is tiny --the name carries a counter and
     * the clock in nanoseconds-- but it exists, and whoever uses this as a lock between processes
     * is in for a surprise. It is the same limitation as {@link #createNewFile()}'s, and for the
     * same reason: the native writes the whole file, it does not open it with flags.
     *
     * @throws IllegalArgumentException if `prefix` has fewer than three characters
     * @throws IOException if after several attempts none could be created
     */
    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        if (prefix == null) {
            throw new NullPointerException();
        }
        if (prefix.length() < 3) {
            throw new IllegalArgumentException("Prefix string \"" + prefix
                    + "\" too short: length must be at least 3");
        }
        String effectiveSuffix = suffix == null ? ".tmp" : suffix;
        File dir = directory;
        if (dir == null) {
            String t = System.getProperty("java.io.tmpdir");
            if (t == null || t.length() == 0) {
                throw new IOException("no temporary directory");
            }
            dir = new File(t);
        }
        // Several attempts and not one: the name might be taken. A fixed number of rounds so that a
        // directory that cannot be written to ends in an exception and not in a hang.
        for (int attempt = 0; attempt < 1000; attempt++) {
            tempSeed = tempSeed * 6364136223846793005L + 1442695040888963407L;
            long n = tempSeed >>> 1;         // unsigned: the name does not carry a minus in front
            File f = new File(dir, prefix + n + effectiveSuffix);
            if (f.exists()) {
                continue;
            }
            if (Fs.writeAllBytes(f.getPath(), new byte[0], false)) {
                return f;
            }
        }
        throw new IOException("Unable to create temporary file in " + dir.getPath());
    }

    /** The same, in the system's temporary directory. */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        return createTempFile(prefix, suffix, null);
    }

    // ---- identity / ordering ----

    public int compareTo(File other) {
        return this.path.compareTo(other.path);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof File)) {
            return false;
        }
        return this.path.equals(((File) obj).path);
    }

    public int hashCode() {
        return this.path.hashCode() ^ 1234321;
    }

    public String toString() {
        return this.path;
    }

    /** A {@link Path} for this abstract path name. */
    public Path toPath() {
        return Path.of(this.path);
    }

    // Collapse mixed/duplicate separators to the platform separator and drop a trailing one.
    private static String normalize(String p) {
        if (p == null) {
            throw new NullPointerException("path cannot be null");
        }
        StringBuilder sb = new StringBuilder();
        char prev = 0;
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            if (c == '/' || c == '\\') {
                c = separatorChar;
            }
            if (!(c == separatorChar && prev == separatorChar)) {
                sb.append(c);
                prev = c;
            }
            i = i + 1;
        }
        int len = sb.length();
        if (len > 1 && sb.charAt(len - 1) == separatorChar) {
            sb.setLength(len - 1);
        }
        return sb.toString();
    }
}
