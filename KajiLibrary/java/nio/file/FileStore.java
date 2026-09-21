package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

// The volume files live on: a partition, a disk, a mount.
//
// This header used to say KajiJDK builds none of these, and that `Files.getFileStore` and
// `FileSystem.getFileStores()` therefore throw. {@link KajiFileStore} is the subclass, in this same
// package, and both of those methods answer: `Fs.diskTotal`/`diskUsable`/`diskUnallocated` arrived
// and the three spaces are real.
//
// What is still not known is the volume's type and whether it is mounted read-only -- see
// `KajiFileStore`, which answers `"unknown"` and `false` and says why neither is a guess.
public abstract class FileStore {

    /** For the subclasses. */
    protected FileStore() {
    }

    /** The volume's name. Its form is system dependent; it may not be unique. */
    public abstract String name();

    /** The filesystem's type: `"ntfs"`, `"ext4"`, `"tmpfs"`. */
    public abstract String type();

    /** Whether it is mounted read-only. */
    public abstract boolean isReadOnly();

    /** The total size, in bytes. */
    public abstract long getTotalSpace() throws IOException;

    /**
     * The bytes this VM can really use.
     *
     * <p>It is different from `getUnallocatedSpace()` and the difference matters: this one
     * discounts the quotas and the space reserved for root, that one does not. It is still an
     * estimate -- between the asking and the writing, another process may have taken it.
     */
    public abstract long getUsableSpace() throws IOException;

    /** The free bytes without discounting quotas or reservations. */
    public abstract long getUnallocatedSpace() throws IOException;

    /**
     * The block size.
     *
     * <p>Concrete and not abstract: the spec gives it a default --failing-- so as not to break the
     * implementations that predate its existence.
     *
     * @throws UnsupportedOperationException if the volume does not know it
     */
    public long getBlockSize() throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Whether the volume supports a file attribute view, by type. */
    public abstract boolean supportsFileAttributeView(Class<? extends FileAttributeView> type);

    /** The same, by view name (`"basic"`, `"posix"`, ...). */
    public abstract boolean supportsFileAttributeView(String name);

    /** A view of the **volume's** attributes, or `null` if it does not support it. */
    public abstract <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type);

    /** A volume attribute by its `"view:attribute"` name. */
    public abstract Object getAttribute(String attribute) throws IOException;
}
