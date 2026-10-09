package java.nio.file.attribute;

import java.io.IOException;

// The `"dos"` view: the `BasicFileAttributes` plus DOS's four bits.
//
// **A difference from the JDK, and it is visible in the signature.** There `readAttributes()`
// returns `DosFileAttributes` --covariant over `BasicFileAttributeView`'s-- and the compiler
// synthesises the bridge that returns `BasicFileAttributes`. It is declared the same here; if our
// own `javac` does not emit that bridge, what is missing is a **synthetic** member, not one of the
// API.
//
// Without an implementation in KajiJDK: there is no native that reads or writes these bits.
public interface DosFileAttributeView extends BasicFileAttributeView {

    /** Always `"dos"`. */
    String name();

    /** The attributes, read in one go. */
    DosFileAttributes readAttributes() throws IOException;

    /** It marks or unmarks the file as read-only. */
    void setReadOnly(boolean value) throws IOException;

    /** It marks or unmarks the file as hidden. */
    void setHidden(boolean value) throws IOException;

    /** It marks or unmarks the file as a system file. */
    void setSystem(boolean value) throws IOException;

    /** It marks or unmarks the archive bit. */
    void setArchive(boolean value) throws IOException;
}
