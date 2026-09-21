package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * KajiLibrary's java.nio.channels.ScatteringByteChannel — reads spreading into several buffers.
 *
 * <p>"Scattering" is literal: a single read fills the first buffer, and what is left over goes to
 * the next. It serves for reading in one go a message with a fixed-length header and a variable
 * body without copying afterwards: one buffer for each part, a single call to the system.
 *
 * <p>It returns `long` and not `int` because the sum of several buffers can overshoot what fits in
 * an `int`.
 */
public interface ScatteringByteChannel extends ReadableByteChannel {

    /**
     * Reads spreading into the given buffers.
     *
     * @param dsts the buffers
     * @param offset the first one to use
     * @param length how many to use
     * @return how many bytes it read in total, or -1 if the channel reached its end
     * @throws IOException if the reading fails
     */
    long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    /** Reads spreading into every buffer. */
    long read(ByteBuffer[] dsts) throws IOException;
}
