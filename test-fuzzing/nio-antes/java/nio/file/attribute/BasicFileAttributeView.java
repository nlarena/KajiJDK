package java.nio.file.attribute;

import java.io.IOException;

// The view with which the `BasicFileAttributes` are read and the three timestamps are set.
//
// Its name is `"basic"` -- fixed, and the one `Files.getAttribute("basic:size", ...)` uses.
//
// **KajiJDK has no implementation.** This note used to say neither reading nor writing is possible
// because `stat` returns no times; reading works --`Files.readAttributes` answers, `Fs.mtime`
// included. What is not possible is `setTimes`, which sets all three at once and only has a native
// for the modification one, and a view whose `setTimes` silently dropped two of the three would be
// worse than no view. The interface is here so the type exists.
public interface BasicFileAttributeView extends FileAttributeView {

    /** Always `"basic"`. */
    String name();

    /** It reads the attributes in one go, so they all come from the same instant. */
    BasicFileAttributes readAttributes() throws IOException;

    /**
     * It sets the timestamps. An argument at `null` leaves that stamp as it was.
     *
     * <p>The three go together, and not one method per stamp, because changing only one usually
     * touches the others by rebound in the filesystem: doing it in one call makes it explicit that
     * the operation is a single one.
     */
    void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
            throws IOException;
}
