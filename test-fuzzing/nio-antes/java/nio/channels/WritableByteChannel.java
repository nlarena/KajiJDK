package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * KajiLibrary's java.nio.channels.WritableByteChannel — a channel bytes can be written into.
 *
 * <p>The counterpart of {@link ReadableByteChannel}. A single method, and its contract has a part
 * that surprises whoever comes from streams: **it may write fewer bytes than there are in the
 * buffer**, and that is why it returns how many it wrote. A non-blocking channel writes what fits
 * now and returns.
 *
 * <p>Hence the right pattern is a loop while `buf.hasRemaining()`, and not a single call.
 */
public interface WritableByteChannel extends Channel {

    /**
     * Writes bytes from the buffer, from its current position.
     *
     * @return how many it wrote, which may be zero
     * @throws IOException if the writing fails
     */
    int write(ByteBuffer src) throws IOException;
}
