package java.nio.file.attribute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// The utilities for going from `"rwxr-xr-x"` to a `Set<PosixFilePermission>` and back.
//
// **It is pure text and that is why it is complete**, even though the package around it cannot read
// permissions off the disk: converting the string needs no native. It is useful on its own --for
// reading a mode out of a configuration file or a tar, say-- and it is also what makes
// `asFileAttribute` mean something the day there is a native that sets permissions on creation.
public final class PosixFilePermissions {

    // It is a utility class: there is nothing to instantiate.
    private PosixFilePermissions() {
    }

    private static void writeTo(StringBuilder sb, Set<PosixFilePermission> perms,
            PosixFilePermission r, PosixFilePermission w, PosixFilePermission x) {
        sb.append(perms.contains(r) ? 'r' : '-');
        sb.append(perms.contains(w) ? 'w' : '-');
        sb.append(perms.contains(x) ? 'x' : '-');
    }

    /**
     * The mode in `ls -l`'s nine letters, without the leading type character: `"rwxr-x---"`.
     *
     * <p>Always nine characters: an absent permission is a dash, not a position that is skipped.
     */
    public static String toString(Set<PosixFilePermission> perms) {
        StringBuilder sb = new StringBuilder(9);
        writeTo(sb, perms, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE);
        writeTo(sb, perms, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE);
        writeTo(sb, perms, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE,
                PosixFilePermission.OTHERS_EXECUTE);
        return sb.toString();
    }

    private static boolean isSet(char c, char expected) {
        if (c == expected) {
            return true;
        }
        if (c == '-') {
            return false;
        }
        throw new IllegalArgumentException("Invalid mode");
    }

    /**
     * `toString`'s inverse.
     *
     * <p>The exact length and the exact letter at each position are required --`r` only in the first
     * of each triple, `w` in the second, `x` in the third. Accepting a plain `"rwx"` or
     * `"xwrxwrxwr"` would be guessing what the caller meant, and a badly written mode string is
     * exactly the place to fail hard.
     *
     * @throws IllegalArgumentException if the string is not nine characters or one of them does not
     *     match its position
     */
    public static Set<PosixFilePermission> fromString(String perms) {
        if (perms.length() != 9) {
            throw new IllegalArgumentException("Invalid mode");
        }
        Set<PosixFilePermission> result = new HashSet<PosixFilePermission>();
        if (isSet(perms.charAt(0), 'r')) {
            result.add(PosixFilePermission.OWNER_READ);
        }
        if (isSet(perms.charAt(1), 'w')) {
            result.add(PosixFilePermission.OWNER_WRITE);
        }
        if (isSet(perms.charAt(2), 'x')) {
            result.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if (isSet(perms.charAt(3), 'r')) {
            result.add(PosixFilePermission.GROUP_READ);
        }
        if (isSet(perms.charAt(4), 'w')) {
            result.add(PosixFilePermission.GROUP_WRITE);
        }
        if (isSet(perms.charAt(5), 'x')) {
            result.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if (isSet(perms.charAt(6), 'r')) {
            result.add(PosixFilePermission.OTHERS_READ);
        }
        if (isSet(perms.charAt(7), 'w')) {
            result.add(PosixFilePermission.OTHERS_WRITE);
        }
        if (isSet(perms.charAt(8), 'x')) {
            result.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        return result;
    }

    // The `FileAttribute` `asFileAttribute` returns. It keeps an immutable copy of the set so the
    // attribute does not change if the caller modifies their own afterwards.
    private static final class PosixAttr implements FileAttribute<Set<PosixFilePermission>> {

        private final Set<PosixFilePermission> perms;

        PosixAttr(Set<PosixFilePermission> perms) {
            this.perms = perms;
        }

        public String name() {
            return "posix:permissions";
        }

        public Set<PosixFilePermission> value() {
            return this.perms;
        }
    }

    /**
     * It wraps the permissions as the `FileAttribute` `Files.createFile` and company take.
     *
     * <p>The set is **copied and checked** here and not on use: if it carries something that is not
     * a `PosixFilePermission` --possible with a raw `Set`-- the error has to turn up where the
     * attribute was built, not inside the file's creation.
     *
     * <p>Mind that KajiJDK cannot honour the attribute. `Files.createFile` with a non-empty
     * `FileAttribute` throws `UnsupportedOperationException`, because the native that creates files
     * takes no permissions.
     */
    public static FileAttribute<Set<PosixFilePermission>> asFileAttribute(
            Set<PosixFilePermission> perms) {
        Set<PosixFilePermission> copied = new HashSet<PosixFilePermission>();
        java.util.Iterator<PosixFilePermission> it = perms.iterator();
        while (it.hasNext()) {
            PosixFilePermission p = it.next();
            if (p == null) {
                throw new NullPointerException();
            }
            copied.add(p);
        }
        return new PosixAttr(Collections.unmodifiableSet(copied));
    }
}
