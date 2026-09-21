package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.TargetDataLine -- a line audio is read from.
 *
 * <p>The <b>input</b>: the microphone, the capture line. It is called "target" because it is the
 * target of the data <b>of the mixer</b>; see {@link SourceDataLine} on this backwards naming.
 *
 * <p>{@link DataLine#start} has to be called after opening: opening reserves the device, starting
 * begins filling the buffer. A line that is open and not started captures nothing, and it is the
 * commonest mistake when recording for the first time.
 *
 * <p>And it has to be read in time: the buffer is circular and gets overwritten. Whatever is not
 * read before it wraps around is lost, without warning.
 */
public interface TargetDataLine extends DataLine {

    /**
     * Opens with that format and that buffer size.
     *
     * <p>Here the buffer decides how long reading can take without losing audio; see the class
     * note.
     *
     * @throws LineUnavailableException if the resource is not available
     * @throws IllegalArgumentException if it does not support that format
     * @throws IllegalStateException if it was already open
     */
    void open(AudioFormat format, int bufferSize) throws LineUnavailableException;

    /** Likewise, with the buffer the device prefers. */
    void open(AudioFormat format) throws LineUnavailableException;

    /**
     * Reads captured audio. It blocks until {@code len} bytes are gathered.
     *
     * @param len it has to be a multiple of the frame size
     * @return how many bytes were read
     * @throws IllegalArgumentException if the length is not a multiple of the frame
     */
    int read(byte[] b, int off, int len);
}
