package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * KajiLibrary's java.nio.channels.GatheringByteChannel — writes gathering several buffers.
 *
 * <p>The symmetric one of {@link ScatteringByteChannel}: a single write empties the first buffer,
 * goes on with the second, and so on. It is what allows sending header and body in a single
 * operation without first building one buffer with everything copied inside.
 */
public interface GatheringByteChannel extends WritableByteChannel {

    /**
     * Writes gathering the given buffers.
     *
     * @param srcs the buffers
     * @param offset the first one to use
     * @param length how many to use
     * @return how many bytes it wrote in total
     * @throws IOException if the writing fails
     */
    long write(ByteBuffer[] srcs, int offset, int length) throws IOException;

    /** Writes gathering every buffer. */
    long write(ByteBuffer[] srcs) throws IOException;
}
