package javax.sound.sampled;

import java.io.IOException;

/**
 * KajiLibrary's javax.sound.sampled.Clip -- audio loaded whole into memory.
 *
 * <p>The difference from {@link SourceDataLine} is one of model, not of quality: that one receives
 * audio in batches while it plays, this one has it all before starting.
 *
 * <p>That is what allows the only thing the other cannot: <b>jumping to a position</b> and
 * <b>looping</b>. A clip is right for a short sound effect that repeats; a long stream goes the
 * other way.
 *
 * <p>The cost is memory: a clip takes the whole audio uncompressed. A minute of 16-bit stereo at
 * 44100 Hz is ten megabytes.
 *
 * <h2>{@link #loop} and the loop points</h2>
 *
 * <p>{@link #setLoopPoints} marks the piece that repeats, in frames. {@code loop(n)} repeats it
 * {@code n} more times, and {@link #LOOP_CONTINUOUSLY} forever.
 *
 * <p>The detail that gets forgotten: {@code loop(0)} is valid and means "do not repeat". It is not
 * the same as not calling {@code loop}, because it starts playback all the same.
 *
 * <p>To cut an endless loop, {@link DataLine#stop} has to be called, or {@code loop(0)} so that the
 * current round finishes and it stops.
 */
public interface Clip extends DataLine {

    /** Repeat forever. */
    int LOOP_CONTINUOUSLY = -1;

    /**
     * Loads audio from a byte array.
     *
     * @param offset from where
     * @param bufferSize how many bytes; it has to be a multiple of the frame
     * @throws LineUnavailableException if the resource is not available
     * @throws IllegalArgumentException if the format is not supported or the length is not a
     *     multiple of the frame
     * @throws IllegalStateException if it was already open
     */
    void open(AudioFormat format, byte[] data, int offset, int bufferSize)
        throws LineUnavailableException;

    /**
     * Loads audio from a stream, up to the end.
     *
     * @throws LineUnavailableException if the resource is not available
     * @throws IOException if it could not be read
     * @throws IllegalArgumentException if the format is not supported
     * @throws IllegalStateException if it was already open
     */
    void open(AudioInputStream stream) throws LineUnavailableException, IOException;

    /** How many frames it has. */
    int getFrameLength();

    /** How long it lasts, in microseconds. */
    long getMicrosecondLength();

    /** Jumps to that frame. */
    void setFramePosition(int frames);

    /** Jumps to that microsecond; it is rounded to the nearest frame. */
    void setMicrosecondPosition(long microseconds);

    /**
     * Marks the piece that repeats.
     *
     * @param end the last frame of the piece; -1 means up to the end
     * @throws IllegalArgumentException if the points are not valid
     */
    void setLoopPoints(int start, int end);

    /**
     * Repeats the marked piece.
     *
     * <p>See the class note: {@code loop(0)} plays without repeating.
     *
     * @param count how many more times, or {@link #LOOP_CONTINUOUSLY}
     */
    void loop(int count);
}
