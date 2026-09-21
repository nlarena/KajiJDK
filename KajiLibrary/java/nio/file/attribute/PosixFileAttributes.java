package java.nio.file.attribute;

import java.util.Set;

// POSIX's attributes: owner, group and the nine permission bits.
//
// Without an implementation in KajiJDK: there is no native that returns uid/gid nor the file's mode.
// Note that `stat` **does** know whether it can be read and written, but that is the permission
// **effective for this process**, which is not the same as the file's bits -- translating one into
// the other (setting `OWNER_READ` because the process can read, say) would be inventing information
// about the group and the others.
public interface PosixFileAttributes extends BasicFileAttributes {

    /** The owner. */
    UserPrincipal owner();

    /** The group. */
    GroupPrincipal group();

    /** The permissions. */
    Set<PosixFilePermission> permissions();
}
