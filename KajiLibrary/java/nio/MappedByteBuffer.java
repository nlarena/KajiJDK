package java.nio;

/**
 * A byte buffer whose contents are a memory-mapped region of a file.
 *
 * <p>Mapping is the point of this class: the operating system makes a file's bytes appear as
 * memory, so reading the buffer reads the file without a copy through the Java heap, and the
 * kernel pages content in and out on demand. That is a capability of the OS reached through the
 * VM, not something a library can synthesise.
 *
 * <p><strong>KajiLibrary cannot map anything.</strong> There is no {@code java.nio.channels},
 * no {@code FileChannel} and no file descriptors, so no instance of this class can be created —
 * the only constructor is package-private and nothing calls it. What is declared here is the
 * surface a caller compiles against; the methods describe what they would do.
 *
 * <p>This is the same shape as {@code ZipFile} in {@code java.util.zip}: an honest hole is better
 * than a class that pretends to hold something it does not.
 */
public abstract class MappedByteBuffer extends ByteBuffer {

    MappedByteBuffer(byte[] hb, int offset, int capacity) {
        super(hb, offset, capacity);
    }

    /**
     * Tells whether this buffer's content is resident in physical memory.
     *
     * @return {@code false}; without a mapping there is nothing resident
     */
    public final boolean isLoaded() {
        return false;
    }

    /**
     * Attempts to load this buffer's content into physical memory.
     *
     * @return this buffer, unchanged
     */
    public final MappedByteBuffer load() {
        return this;
    }

    /**
     * Writes any changes back to the file that contains the mapped region.
     *
     * @return this buffer, unchanged
     */
    public final MappedByteBuffer force() {
        return this;
    }
}
