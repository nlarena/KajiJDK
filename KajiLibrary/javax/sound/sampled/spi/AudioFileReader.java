package javax.sound.sampled.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * KajiLibrary's javax.sound.sampled.spi.AudioFileReader -- reads audio files of a format.
 *
 * <p>What is implemented by whoever adds support for reading a new format. It is registered as a
 * service and {@code AudioSystem} finds it by itself.
 *
 * <h2>The rule of marks</h2>
 *
 * <p>{@code AudioSystem} tries the readers <b>one at a time</b> on the same stream. That is why a
 * reader has to mark before looking at the bytes and <b>rewind</b> if it does not recognize the
 * format: otherwise the next reader receives an already consumed stream and none works.
 *
 * <p>Not recognizing a format is signalled with {@link UnsupportedAudioFileException}, which
 * {@code AudioSystem} catches to go on with the next one. Returning null is not allowed.
 *
 * <p>The six operations are three times two: format or stream, from a file, a URL or a stream.
 */
public abstract class AudioFileReader {

    /** For the subclasses. */
    protected AudioFileReader() {
    }

    /**
     * What there is in that stream.
     *
     * <p>It has to leave it as it found it; see the class note.
     *
     * @throws UnsupportedAudioFileException if this reader does not recognize the format
     * @throws IOException if it could not be read
     */
    public abstract AudioFileFormat getAudioFileFormat(InputStream stream)
        throws UnsupportedAudioFileException, IOException;

    /**
     * Likewise, from a URL.
     *
     * @throws UnsupportedAudioFileException if this reader does not recognize the format
     * @throws IOException if it could not be read
     */
    public abstract AudioFileFormat getAudioFileFormat(URL url)
        throws UnsupportedAudioFileException, IOException;

    /**
     * Likewise, from a file.
     *
     * @throws UnsupportedAudioFileException if this reader does not recognize the format
     * @throws IOException if it could not be read
     */
    public abstract AudioFileFormat getAudioFileFormat(File file)
        throws UnsupportedAudioFileException, IOException;

    /**
     * An audio stream from that byte stream.
     *
     * @throws UnsupportedAudioFileException if this reader does not recognize the format
     * @throws IOException if it could not be read
     */
    public abstract AudioInputStream getAudioInputStream(InputStream stream)
        throws UnsupportedAudioFileException, IOException;

    /**
     * Likewise, from a URL.
     *
     * @throws UnsupportedAudioFileException if this reader does not recognize the format
     * @throws IOException if it could not be read
     */
    public abstract AudioInputStream getAudioInputStream(URL url)
        throws UnsupportedAudioFileException, IOException;

    /**
     * Likewise, from a file.
     *
     * @throws UnsupportedAudioFileException if this reader does not recognize the format
     * @throws IOException if it could not be read
     */
    public abstract AudioInputStream getAudioInputStream(File file)
        throws UnsupportedAudioFileException, IOException;
}
