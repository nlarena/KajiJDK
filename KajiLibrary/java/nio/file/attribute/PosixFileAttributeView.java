package java.nio.file.attribute;

import java.io.IOException;
import java.util.Set;

// The `"posix"` view: the basic attributes, plus owner, group and the nine permission bits.
//
// It inherits from two interfaces at once --`BasicFileAttributeView` for the times,
// `FileOwnerAttributeView` for the owner-- which is what makes `getOwner()` available without
// redeclaring it.
//
// Without an implementation in KajiJDK: see `PosixFileAttributes`'s note on why the effective
// permissions `stat` does know are not enough to reconstruct the file's bits.
public interface PosixFileAttributeView extends BasicFileAttributeView, FileOwnerAttributeView {

    /** Always `"posix"`. */
    String name();

    /** The attributes, read in one go. */
    PosixFileAttributes readAttributes() throws IOException;

    /** It changes the nine permission bits. */
    void setPermissions(Set<PosixFilePermission> perms) throws IOException;

    /** It changes the group. */
    void setGroup(GroupPrincipal group) throws IOException;
}
