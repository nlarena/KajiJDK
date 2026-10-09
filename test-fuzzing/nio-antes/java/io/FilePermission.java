package java.io;

import java.security.Permission;
import java.security.PermissionCollection;

// KajiLibrary's java.io.FilePermission -- the permission to touch files, with its grammar of paths.
//
// **It is pure logic and that is why it can be implemented in full.** A permission opens no files:
// it answers "does what I have cover what is being asked for?". That reckoning is done over two
// strings and touches no disk, so it depends on nothing this VM lacks.
//
// **A note on the model's state**, the same one as `java.security.Permission`'s: since JDK 24 the
// `SecurityManager` has been permanently disabled, so nobody consults these permissions at run
// time. They are implemented because they are contract -- they appear in signatures other code
// names -- not because they enforce anything. JDK 25 marks it deprecated and for removal, and that
// mark is reproduced here instead of being left out: whoever uses it has to see the same warning
// they would see compiling against the JDK.
//
// <h2>The grammar of names</h2>
//
// <pre>
//   "&lt;&lt;ALL FILES&gt;&gt;"  the whole file system
//   "/tmp/x"          exactly that file
//   "/tmp/*"          the files **directly** inside /tmp, not those of its subdirectories
//   "/tmp/-"          /tmp and everything hanging off it, at any depth
//   "*" / "-"         likewise over the current directory
// </pre>
//
// Two details of the contract that look arbitrary and are not:
//
//   - `"/tmp/-"` does **not** imply `"/tmp"`. The wildcard talks about what is *inside*; the
//     directory itself is another object, and deleting it is not the same as deleting its contents.
//   - `"/tmp/-"` does imply `"/tmp/*"` -- the recursive contains the flat -- but not the other way
//     round.
//
// <h2>Paths are normalized, and not canonicalized</h2>
//
// Before comparing, the path is normalized: the two separators are unified, the repeated ones are
// collapsed, and the `.` and `..` segments are resolved **lexically**. That is why `"a.txt"`
// implies `"./a.txt"`.
//
// What is **not** done is resolving the path against the current directory or following links:
// `"/tmp/a"` and `"tmp/a"` do not imply each other, even though in a given session they might name
// the same file. It is the same thing the JDK has done since 9
// (`jdk.io.permissionsUseCanonicalPath` false by default), and the reason is that canonicalizing
// touches the disk: the result would depend on the file existing and on where a link pointed **at
// the moment the permission was constructed**, so the same permission could imply different things
// in two runs. A permission has to be a stable decision.
@Deprecated(since = "24", forRemoval = true)
public final class FilePermission extends Permission implements Serializable {

    private static final int READ = 1;
    private static final int WRITE = 2;
    private static final int EXECUTE = 4;
    private static final int DELETE = 8;
    private static final int READLINK = 16;

    private static final String ALL_FILES = "<<ALL FILES>>";

    // The granted actions, as bits. The mask is stored and not the string because the question
    // asked a thousand times is "are these included?", which in bits is an `and`.
    private final int mask;

    private final boolean allFiles;

    // The path's wildcard: `directory` for `/*`, and `recursive` as well for `/-`. They are two
    // flags and not an enum of three because `recursive` implies `directory`, and keeping them
    // apart makes the four combinations of `implies` read exactly as they are written in the
    // contract.
    private final boolean directory;
    private final boolean recursive;

    // The normalized path, already without the trailing wildcard: the root on one side (`""`,
    // `"\"`, `"C:\"`) and the segments on the other. Split like that because `implies`'s two
    // questions are "same root?" and "is it a prefix of the segments?", and over the whole string
    // the second would give false positives -- `/tmpx/a` starts with `/tmp` as text and is not
    // inside `/tmp`.
    private final String root;
    private final String[] segments;

    /**
     * A permission over `path` for `actions`.
     *
     * @throws NullPointerException if `path` is `null`
     * @throws IllegalArgumentException if `actions` is `null`, is empty, or names anything other
     *     than `read`, `write`, `execute`, `delete` or `readlink`. It is rejected instead of
     *     ignored: a misspelt action silently discarded would give a narrower permission than the
     *     one that was meant, and that is only discovered the day it denies something.
     */
    public FilePermission(String path, String actions) {
        super(path);
        if (path == null) {
            throw new NullPointerException("name can't be null");
        }
        this.mask = maskOf(actions);

        if (path.equals(ALL_FILES)) {
            this.allFiles = true;
            this.directory = false;
            this.recursive = false;
            this.root = "";
            this.segments = new String[0];
            return;
        }
        this.allFiles = false;

        // The wildcard is taken off **before** normalizing: normalizing first could move the `-`
        // elsewhere while resolving a `..` that came right before it.
        String raw = path;
        boolean dir = false;
        boolean rec = false;
        if (raw.equals("*")) {
            dir = true;
            raw = "";
        } else if (raw.equals("-")) {
            dir = true;
            rec = true;
            raw = "";
        } else if (raw.length() >= 2 && isSeparator(raw.charAt(raw.length() - 2))) {
            char last = raw.charAt(raw.length() - 1);
            if (last == '*') {
                dir = true;
                raw = raw.substring(0, raw.length() - 1);
            } else if (last == '-') {
                dir = true;
                rec = true;
                raw = raw.substring(0, raw.length() - 1);
            }
        }
        this.directory = dir;
        this.recursive = rec;

        this.root = rootOf(raw);
        this.segments = segmentsOf(raw.substring(this.root.length()));
    }

    /**
     * Whether this permission covers `p`.
     *
     * <p>They are two independent questions and both have to answer yes: that `p`'s actions are all
     * included in this one's, and that `p`'s path falls inside this one's.
     */
    public boolean implies(Permission p) {
        if (!(p instanceof FilePermission)) {
            return false;
        }
        FilePermission other = (FilePermission) p;
        if ((this.mask & other.mask) != other.mask) {
            return false;
        }
        return this.coversThePathOf(other);
    }

    // The half of `implies` that looks only at the path.
    //
    // The four cases come from the contract and are written out one by one on purpose: collapsing
    // them into a prefix comparison with one extra `if` is where `/tmp/-` implying `/tmp` sneaks
    // in, which is exactly what must not happen.
    private boolean coversThePathOf(FilePermission other) {
        if (this.allFiles) {
            return true;
        }
        if (other.allFiles) {
            return false;               // only the universal wildcard implies itself
        }
        if (!this.root.equals(other.root)) {
            return false;               // absolute and relative do not compare; see the class note
        }
        int mine = this.segments.length;
        int theirs = other.segments.length;

        if (!this.directory) {
            // A bare path implies exactly itself.
            return !other.directory && theirs == mine && this.isPrefixOf(other);
        }
        if (this.recursive) {
            if (other.directory) {
                // `/tmp/-` covers `/tmp/*` and `/tmp/sub/-`: any wildcard further down.
                return theirs >= mine && this.isPrefixOf(other);
            }
            // Strict: `/tmp/-` talks about what is inside /tmp, and /tmp is not inside /tmp.
            return theirs > mine && this.isPrefixOf(other);
        }
        if (other.directory) {
            // `/tmp/*` covers only the same `/tmp/*`, and never a `-`, which is wider.
            return !other.recursive && theirs == mine && this.isPrefixOf(other);
        }
        // Exactly one level: `/tmp/*` covers `/tmp/a.txt` and not `/tmp/sub/a.txt`.
        return theirs == mine + 1 && this.isPrefixOf(other);
    }

    private boolean isPrefixOf(FilePermission other) {
        if (other.segments.length < this.segments.length) {
            return false;
        }
        int i = 0;
        while (i < this.segments.length) {
            if (!this.segments[i].equals(other.segments[i])) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * The actions in canonical order: `read,write,execute,delete,readlink`.
     *
     * <p>Canonical and not the order they were written in, so that two equal permissions look
     * equal: `"write,read"` and `"read,write"` grant the same thing and have to print the same,
     * otherwise `equals` and `toString` would tell different stories.
     */
    public String getActions() {
        StringBuilder sb = new StringBuilder();
        if ((this.mask & READ) != 0) {
            sb.append("read");
        }
        if ((this.mask & WRITE) != 0) {
            comma(sb);
            sb.append("write");
        }
        if ((this.mask & EXECUTE) != 0) {
            comma(sb);
            sb.append("execute");
        }
        if ((this.mask & DELETE) != 0) {
            comma(sb);
            sb.append("delete");
        }
        if ((this.mask & READLINK) != 0) {
            comma(sb);
            sb.append("readlink");
        }
        return sb.toString();
    }

    /**
     * Two permissions are equal if they grant the same thing over the same path.
     *
     * <p>The **normalized** path is compared and not the name as it was written, for the same
     * reason as in `implies`: `"/tmp/./a"` and `"/tmp/a"` are the same permission, and if they were
     * not equal a collection would store both and answer the same thing twice.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FilePermission)) {
            return false;
        }
        FilePermission other = (FilePermission) obj;
        if (this.mask != other.mask
                || this.allFiles != other.allFiles
                || this.directory != other.directory
                || this.recursive != other.recursive
                || !this.root.equals(other.root)
                || this.segments.length != other.segments.length) {
            return false;
        }
        return this.isPrefixOf(other);
    }

    public int hashCode() {
        int h = this.root.hashCode();
        int i = 0;
        while (i < this.segments.length) {
            h = h * 31 + this.segments[i].hashCode();
            i = i + 1;
        }
        h = h * 31 + this.mask;
        h = h * 31 + (this.directory ? 2 : 0) + (this.recursive ? 1 : 0);
        return h * 31 + (this.allFiles ? 1 : 0);
    }

    /**
     * A collection for gathering file permissions.
     *
     * <p>It does not index by name the way `BasicPermission` does, and that is no omission: there
     * the wildcard always falls in a predictable place (`a.b.*`) and the four candidates can be
     * tried; here `"/a/-"` may cover `"/a/b/c/d"` at any depth, so there is no small set of keys to
     * look up. It is walked.
     */
    public PermissionCollection newPermissionCollection() {
        return new FilePermissionCollection();
    }

    // ---- splitting and normalizing paths -----------------------------------------------------

    private static boolean isSeparator(char c) {
        return c == '/' || c == '\\';
    }

    // The root: `""` (relative), `"\"` (absolute), `"C:"` (relative to a drive) or `"C:\"`.
    //
    // The distinction between `"C:"` and `"C:\"` is kept because they are different things on
    // Windows: the first is relative to that **drive's** current directory. Merging them would make
    // a permission over one imply the other.
    private static String rootOf(String p) {
        int i = 0;
        int n = p.length();
        if (n >= 2 && p.charAt(1) == ':' && isLetter(p.charAt(0))) {
            i = 2;
        }
        StringBuilder sb = new StringBuilder(p.substring(0, i));
        if (i < n && isSeparator(p.charAt(i))) {
            sb.append(File.separatorChar);
            while (i < n && isSeparator(p.charAt(i))) {
                i = i + 1;
            }
        }
        return sb.toString();
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    // The segments of the rootless part, with `.` discarded and `..` resolved lexically.
    private static String[] segmentsOf(String rest) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int i = 0;
        int n = rest.length();
        while (i < n) {
            int j = i;
            while (j < n && !isSeparator(rest.charAt(j))) {
                j = j + 1;
            }
            if (j > i) {
                String seg = rest.substring(i, j);
                if (seg.equals(".")) {
                    // nothing: `.` is "right here"
                } else if (seg.equals("..")) {
                    // If there is nobody to climb to, the `..` is kept: discarding it would turn
                    // `../secret` into `secret`, that is, into another file.
                    if (!out.isEmpty() && !out.get(out.size() - 1).equals("..")) {
                        out.remove(out.size() - 1);
                    } else {
                        out.add("..");
                    }
                } else {
                    out.add(seg);
                }
            }
            i = j + 1;
        }
        return out.toArray(new String[out.size()]);
    }

    // ---- actions -----------------------------------------------------------------------------

    private static void comma(StringBuilder sb) {
        if (sb.length() > 0) {
            sb.append(',');
        }
    }

    private static int maskOf(String actions) {
        if (actions == null) {
            throw new IllegalArgumentException("actions can't be null");
        }
        int m = 0;
        int i = 0;
        int n = actions.length();
        while (i <= n) {
            int j = i;
            while (j < n && actions.charAt(j) != ',') {
                j = j + 1;
            }
            String piece = actions.substring(i, j).trim();
            if (piece.length() > 0) {
                m = m | oneAction(piece);
            } else if (n > 0) {
                // `"read,,write"` or `"read,"`: a comma with no action is a writing mistake, and
                // accepting it would hide the one that really matters -- a mistyped action next to
                // it.
                throw new IllegalArgumentException("invalid actions: " + actions);
            }
            i = j + 1;
        }
        if (m == 0) {
            throw new IllegalArgumentException("invalid actions: " + actions);
        }
        return m;
    }

    private static int oneAction(String s) {
        String a = s.toLowerCase();
        if (a.equals("read")) {
            return READ;
        }
        if (a.equals("write")) {
            return WRITE;
        }
        if (a.equals("execute")) {
            return EXECUTE;
        }
        if (a.equals("delete")) {
            return DELETE;
        }
        if (a.equals("readlink")) {
            return READLINK;
        }
        throw new IllegalArgumentException("invalid actions: " + s);
    }
}
