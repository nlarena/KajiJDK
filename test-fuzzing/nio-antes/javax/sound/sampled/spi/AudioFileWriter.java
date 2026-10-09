package javax.sound.sampled.spi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * KajiLibrary's javax.sound.sampled.spi.AudioFileWriter -- writes audio files of a format.
 *
 * <p>The mirror of {@link AudioFileReader}. It is registered the same way and {@code AudioSystem}
 * finds it the same way.
 *
 * <h2>The two {@code getAudioFileTypes}</h2>
 *
 * <p>The one without arguments says which types it can write <b>in general</b>; the one that takes
 * a stream says which it can write <b>with that content</b>. The difference matters: a writer can
 * know how to do WAV and not be able to save to WAV a stream of unknown length, because the WAV
 * header carries the size and it has to be known beforehand.
 *
 * <p>The two {@code isFileTypeSupported} come implemented over the previous ones; a subclass need
 * not touch them.
 */
public abstract class AudioFileWriter {

    /** For the subclasses. */
    protected AudioFileWriter() {
    }

    /** Which types it can write in general. See the class note. */
    public abstract AudioFileFormat.Type[] getAudioFileTypes();

    /** Whether it can write that type. */
    public boolean isFileTypeSupported(AudioFileFormat.Type fileType) {
        AudioFileFormat.Type[] types = getAudioFileTypes();
        int i = 0;
        while (types != null && i < types.length) {
            if (fileType.equals(types[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** Which types it can write with that content. See the class note. */
    public abstract AudioFileFormat.Type[] getAudioFileTypes(AudioInputStream stream);

    /** Whether it can write that type with that content. */
    public boolean isFileTypeSupported(AudioFileFormat.Type fileType, AudioInputStream stream) {
        AudioFileFormat.Type[] types = getAudioFileTypes(stream);
        int i = 0;
        while (types != null && i < types.length) {
            if (fileType.equals(types[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Writes.
     *
     * @return how many bytes were written
     * @throws IOException if it could not be written
     * @throws IllegalArgumentException if it does not support that type with that content
     */
    public abstract int write(AudioInputStream stream, AudioFileFormat.Type fileType,
                              OutputStream out) throws IOException;

    /**
     * Likewise, to a file.
     *
     * @return how many bytes were written
     * @throws IOException if it could not be written
     * @throws IllegalArgumentException if it does not support that type with that content
     */
    public abstract int write(AudioInputStream stream, AudioFileFormat.Type fileType, File out)
        throws IOException;
}
