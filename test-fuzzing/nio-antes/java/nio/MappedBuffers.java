package java.nio;

/**
 * Builds the mapped byte buffers that {@code java.nio.channels.FileChannel.map} hands out.
 *
 * <p>Not a JDK class: it is scaffolding of ours, the same kind as
 * {@code java.nio.channels.MapModes}. The implementation is package-private because nobody
 * outside builds one and a public name here would be a name {@code java.nio} does not have in the
 * JDK; but {@code FileChannel} lives in another package and cannot see it. This is the one bridge.
 */
public final class MappedBuffers {

    private MappedBuffers() {
    }

    /**
     * A buffer over a mapping the VM already made.
     *
     * @param token the mapping token, from {@code jdk.internal.io.Fs.mapOpen}
     * @param capacity how many bytes the mapping spans
     * @param readOnly whether the buffer refuses writes
     * @return the buffer, positioned at zero
     */
    public static MappedByteBuffer of(int token, int capacity, boolean readOnly) {
        return new MappedBuffer(token, 0, capacity, readOnly);
    }
}
