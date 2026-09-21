package java.nio.file.attribute;

import java.io.IOException;

// The view that knows how to read and change a file's owner. Its name is `"owner"`, except when it
// is reached through `PosixFileAttributeView` or `AclFileAttributeView`, which inherit it and return
// their own.
//
// Without an implementation in KajiJDK: there is no native that queries or changes the owner.
public interface FileOwnerAttributeView extends FileAttributeView {

    /** `"owner"`, or the name of the view that extends it. */
    String name();

    /** The owner. */
    UserPrincipal getOwner() throws IOException;

    /** It changes the owner. */
    void setOwner(UserPrincipal owner) throws IOException;
}
