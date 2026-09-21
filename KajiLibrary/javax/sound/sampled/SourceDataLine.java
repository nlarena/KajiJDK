package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.SourceDataLine -- a line audio is written to.
 *
 * <p>The name confuses and it is worth clearing up at once: it is the <b>output</b>. It is called
 * "source" because it is the source of data <b>of the mixer</b>, not of the program. The input, the
 * one that captures, is {@link TargetDataLine}.
 *
 * <p>{@link #write} blocks until everything passed to it has gone in, and that is what sets the
 * pace: the program advances at the speed at which the device consumes. It is the right way to play
 * without building up delay or cutting out.
 *
 * <p>It always writes whole frames; a length that is not a multiple of the frame throws
 * {@link IllegalArgumentException}.
 */
public interface SourceDataLine extends DataLine {

    /**
     * Opens with that format and that buffer size.
     *
     * <p>The buffer is a <b>suggestion</b>: the device can give it another. A small buffer lowers
     * the latency and raises the risk of dropouts.
     *
     * @throws LineUnavailableException if the resource is not available
     * @throws IllegalArgumentException if it does not support that format
     * @throws IllegalStateException if it was already open
     */
    void open(AudioFormat format, int bufferSize) throws LineUnavailableException;

    /** Likewise, with the buffer the device prefers. */
    void open(AudioFormat format) throws LineUnavailableException;

    /**
     * Writes audio. It blocks; see the class note.
     *
     * @param len it has to be a multiple of the frame size
     * @return how many bytes were written
     * @throws IllegalArgumentException if the length is not a multiple of the frame
     */
    int write(byte[] b, int off, int len);
}
