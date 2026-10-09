package java.util.prefs;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import jdk.internal.io.Fs;

// The default store: one directory per node, one file per key table.
//
// WHY THIS SHAPE. One file per node and not a single one with the whole tree, because two parts of
// the program writing into different nodes do not step on each other and a failed write ruins one
// node and not the entire tree. One directory per node and not a path encoded inside the name,
// because that way `childrenNamesSpi` is an `Fs.list` and removing a subtree is the bottom-up
// removal {@link AbstractPreferences} already does.
//
// THE DIRECTORY'S NAME IS NOT THE NODE'S. A node name may hold any character but `/` and is case
// sensitive; a directory name on Windows is **not** case sensitive and forbids `< > : " \ | ? *`,
// the reserved names (`con`, `nul`, `lpt1`...) and a trailing dot. If the name were used as it is,
// the nodes `Data` and `data` --which the contract says are different-- would be the same directory,
// which is silent data loss. That is why a name that does not fall in the safe set `[a-z0-9.-]` is
// written as `_` followed by the hexadecimal of its UTF-16 units. Ordinary names stay readable and
// the rest stays correct; that what is mangled starts with `_` and what is not never does is what
// makes the way back unambiguous.
//
// WHEN IT WRITES. There is no background synchronisation thread --the JDK has one that runs every
// thirty seconds-- so "eventually" would never arrive. Instead every mutation writes **right then**,
// and if that write fails the failure is noted and `flush()` or `sync()` report it, which are the
// only methods of the contract that may throw. That way `put()` still does not throw --as the
// contract demands-- and even so nothing is lost to a power cut between the `put` and the `flush`
// the program may never make.
final class FilePreferences extends AbstractPreferences {

    // The file with this node's keys. It starts with `_`, so no child directory, mangled or not, can
    // be called the same.
    private static final String KEY_FILE = "_prefs";

    private final String dir;

    // `null` while the file has not been read. The read is lazy on purpose: materialising a node to
    // ask a child its name has no reason to touch the disk.
    private Map<String, String> values;

    // There are changes in memory that are not on disk.
    private boolean dirty;

    // Why they are not, or `null` if they are. The only thing that reads it is `flushSpi`.
    private String failure;

    // The node has already been removed from disk; `flushSpi` must not recreate the directory.
    private boolean wasRemoved;

    // The root of a tree, hanging off `dir`.
    FilePreferences(String dir) {
        super(null, "");
        this.dir = dir;
    }

    private FilePreferences(FilePreferences parent, String name) {
        super(parent, name);
        this.dir = parent.dir + "/" + toDirectory(name);
        // New if there was no directory: it is what decides whether `childAdded` goes out, and
        // asking the disk is the only answer that is not made up.
        this.newNode = !isDirectory(this.dir);
    }

    // ---- the nine of the Spi ----------------------------------------------------------------

    protected String getSpi(String key) {
        load();
        return values.get(key);
    }

    protected void putSpi(String key, String value) {
        load();
        values.put(key, value);
        dirty = true;
        writeWithoutThrowing();
    }

    protected void removeSpi(String key) {
        load();
        if (values.remove(key) != null) {
            dirty = true;
            writeWithoutThrowing();
        }
    }

    protected String[] keysSpi() throws BackingStoreException {
        load();
        return values.keySet().toArray(new String[0]);
    }

    protected String[] childrenNamesSpi() throws BackingStoreException {
        if (!isDirectory(dir)) {
            // A node not yet written has no children on disk. It is not a failure of the store and
            // returning an empty list is the exact answer.
            return new String[0];
        }
        String[] entries = Fs.list(dir);
        if (entries == null) {
            throw new BackingStoreException("could not list " + dir);
        }
        ArrayList<String> names = new ArrayList<String>();
        for (int i = 0; i < entries.length; i++) {
            if (!isDirectory(dir + "/" + entries[i])) {
                continue; // the key file, or junk we did not put there
            }
            String n = toName(entries[i]);
            if (n != null) {
                names.add(n);
            }
        }
        return names.toArray(new String[0]);
    }

    protected AbstractPreferences childSpi(String name) {
        return new FilePreferences(this, name);
    }

    protected void removeNodeSpi() throws BackingStoreException {
        String file = dir + "/" + KEY_FILE;
        if (exists(file) && !Fs.delete(file)) {
            throw new BackingStoreException("could not delete " + file);
        }
        if (isDirectory(dir) && !Fs.delete(dir)) {
            throw new BackingStoreException("could not delete the directory " + dir);
        }
        values = new TreeMap<String, String>();
        dirty = false;
        failure = null;
        wasRemoved = true;
    }

    protected void flushSpi() throws BackingStoreException {
        if (wasRemoved) {
            return;
        }
        // The directory is created even with not a single key: it is what makes the node exist for
        // the next VM, and `node("x"); flush();` has to leave it existing.
        write();
    }

    protected void syncSpi() throws BackingStoreException {
        if (wasRemoved) {
            return;
        }
        write();
        // Only now is what was read thrown away: the other way round would lose the changes that
        // have not gone down yet.
        values = null;
    }

    // ---- disk -----------------------------------------------------------------------------------

    private void load() {
        if (values != null) {
            return;
        }
        Map<String, String> m = new TreeMap<String, String>();
        byte[] b = Fs.readAllBytes(dir + "/" + KEY_FILE);
        if (b != null) {
            read(new String(b, java.nio.charset.StandardCharsets.ISO_8859_1), m);
        }
        values = m;
    }

    private void writeWithoutThrowing() {
        try {
            write();
        } catch (BackingStoreException e) {
            // There is no way to report it: `put` and `remove` do not throw. It is noted for the
            // `flush`.
            failure = e.getMessage();
        }
    }

    private void write() throws BackingStoreException {
        if (!isDirectory(dir)) {
            Fs.mkdir(dir, true);
            if (!isDirectory(dir)) {
                // This is where the program learns there is nowhere to store anything: the VM has
                // neither `user.home` nor `java.io.tmpdir`, so if the working directory does not let
                // itself be written to there is no store left.
                throw new BackingStoreException("could not create the directory " + dir);
            }
        }
        if (!dirty && failure == null) {
            return;
        }
        load();
        byte[] b = writeTable().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        if (!Fs.writeAllBytes(dir + "/" + KEY_FILE, b, false)) {
            String m = "could not write " + dir + "/" + KEY_FILE;
            failure = m;
            throw new BackingStoreException(m);
        }
        dirty = false;
        failure = null;
    }

    private static boolean exists(String path) {
        return (Fs.stat(path) & Fs.EXISTS) != 0;
    }

    private static boolean isDirectory(String path) {
        return (Fs.stat(path) & Fs.IS_DIRECTORY) != 0;
    }

    // ---- the file's format --------------------------------------------------------------------
    //
    // One line per entry, `key=value`, with everything that is not printable ASCII written as
    // `\uXXXX`. It is escaped like that and not stored as raw UTF-8 for a concrete reason: the file
    // stays in seven-bit ASCII and then does not depend on which encoding the next VM opens it with
    // --nor an editor, nor a `type`, nor a `cat`-- which is exactly the kind of error that turns up
    // months later and on somebody else's machine. It is the same decision `java.util.Properties`
    // takes.
    //
    // It is not the JDK's XML. That format is `exportSubtree`'s, which is an *interchange* format and
    // is implemented in {@link Xml}; the one inside the store is seen by nobody outside and has no
    // reason to pay for a parser.

    private String writeTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("# KajiJDK java.util.prefs -- ").append(absolutePath()).append('\n');
        for (Map.Entry<String, String> e : values.entrySet()) {
            writeEscaped(sb, e.getKey());
            sb.append('=');
            writeEscaped(sb, e.getValue());
            sb.append('\n');
        }
        return sb.toString();
    }

    private static void writeEscaped(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '=') {
                sb.append("\\=");
            } else if (c == '#') {
                // It only gets in the way at the start of the line, but escaping it always saves
                // having to know which position we are at.
                sb.append("\\#");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c >= 0x20 && c <= 0x7e) {
                sb.append(c);
            } else {
                sb.append("\\u");
                for (int d = 12; d >= 0; d -= 4) {
                    sb.append(HEX[(c >> d) & 0xf]);
                }
            }
        }
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static void read(String text, Map<String, String> m) {
        int i = 0;
        int n = text.length();
        while (i < n) {
            int end = text.indexOf('\n', i);
            if (end < 0) {
                end = n;
            }
            String line = text.substring(i, end);
            i = end + 1;
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            int cut = separator(line);
            if (cut < 0) {
                continue; // broken line: skipped, and the rest is kept
            }
            m.put(unescape(line.substring(0, cut)), unescape(line.substring(cut + 1)));
        }
    }

    // The position of the first `=` that does not come escaped.
    private static int separator(String line) {
        boolean escape = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escape) {
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '=') {
                return i;
            }
        }
        return -1;
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (++i >= s.length()) {
                break;
            }
            char e = s.charAt(i);
            if (e == 'n') {
                sb.append('\n');
            } else if (e == 'r') {
                sb.append('\r');
            } else if (e == 'u' && i + 4 < s.length()) {
                sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                i += 4;
            } else {
                sb.append(e);
            }
        }
        return sb.toString();
    }

    // ---- the directory's name     -----------------------------------------------------------

    // The names Windows reserves for devices. A directory called one of those cannot be created, and
    // the error it gives looks nothing like the cause.
    private static final String[] RESERVED = {
        "con", "prn", "aux", "nul",
        "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
        "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
    };

    static String toDirectory(String name) {
        if (safe(name)) {
            return name;
        }
        StringBuilder sb = new StringBuilder(1 + name.length() * 4);
        sb.append('_');
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            for (int d = 12; d >= 0; d -= 4) {
                sb.append(HEX[(c >> d) & 0xf]);
            }
        }
        return sb.toString();
    }

    // `null` if the entry is not a directory we wrote ourselves.
    static String toName(String dir) {
        if (dir.length() == 0) {
            return null;
        }
        if (dir.charAt(0) != '_') {
            return safe(dir) ? dir : null;
        }
        String hex = dir.substring(1);
        if (hex.length() == 0 || hex.length() % 4 != 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(hex.length() / 4);
        for (int i = 0; i < hex.length(); i += 4) {
            int v = 0;
            for (int j = 0; j < 4; j++) {
                int d = Character.digit(hex.charAt(i + j), 16);
                if (d < 0) {
                    return null;
                }
                v = (v << 4) | d;
            }
            sb.append((char) v);
        }
        return sb.toString();
    }

    private static boolean safe(String name) {
        int n = name.length();
        if (n == 0 || name.charAt(0) == '_' || name.charAt(n - 1) == '.') {
            return false;
        }
        for (int i = 0; i < n; i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '-';
            // Mind what is NOT there: upper case (Windows does not distinguish it in a directory)
            // and `_` (which is the mark for "this comes mangled").
            if (!ok) {
                return false;
            }
        }
        int dot = name.indexOf('.');
        String base = dot < 0 ? name : name.substring(0, dot);
        if (base.length() == 0) {
            return false; // "." and ".." come in here
        }
        for (int i = 0; i < RESERVED.length; i++) {
            if (base.equals(RESERVED[i])) {
                return false;
            }
        }
        return true;
    }
}
