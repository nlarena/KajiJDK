package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * KajiLibrary's java.nio.channels.SeekableByteChannel — a channel with a **position**.
 *
 * <p>It is the difference between a file and a socket: in a file one can go and come back, in a
 * socket the bytes pass only once. Everything this type adds --{@link #position()},
 * {@link #truncate}, {@link #size()}-- only makes sense over something that can be walked.
 *
 * <p>It redeclares `read` and `write` although it inherits them already, just as the JDK does, and
 * it is not redundant: it is where it is documented that **both advance the position**, which is
 * what tells them apart from those of the ordinary channel.
 *
 * <p>The implementation this library brings is {@link FileChannel}, which is obtained with {@link
 * FileChannel#open} or with `java.nio.file.Files.newByteChannel`. The position there is
 * **simulated**: the natives of this VM read and write the whole file, so the channel keeps count
 * on its own and every operation walks it all. It comes out expensive and it does not lie, which is
 * the deal the header of {@link FileChannel} explains.
 */
public interface SeekableByteChannel extends ByteChannel {

    /** Reads from the current position and advances it. */
    int read(ByteBuffer dst) throws IOException;

    /** Writes from the current position and advances it. */
    int write(ByteBuffer src) throws IOException;

    /** The current position, in bytes from the beginning. */
    long position() throws IOException;

    /**
     * Moves the position.
     *
     * <p>**Beyond the end** is admitted: it is not an error, and reading there returns -1. Writing
     * there leaves a hole, which is how sparse files are made.
     */
    SeekableByteChannel position(long newPosition) throws IOException;

    /** The current size, in bytes. */
    long size() throws IOException;

    /**
     * Cuts the contents down to that size.
     *
     * <p>If the position was left beyond the new end, it becomes the new end: it cannot be left
     * pointing outside what exists.
     */
    SeekableByteChannel truncate(long size) throws IOException;
}
