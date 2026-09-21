package javax.sound.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Soundbank;

/**
 * KajiLibrary's javax.sound.midi.spi.SoundbankReader -- reads sound banks.
 *
 * <p>SoundFont, DLS, or a manufacturer's own format. It is registered as a service and
 * {@code MidiSystem} finds it by itself.
 *
 * <p>Unlike {@link MidiFileReader}, here "I do not recognize it" is signalled by returning
 * <b>null</b>, not by throwing. {@code MidiSystem} goes on with the next reader and only throws if
 * none could.
 *
 * <p>It is an inconsistency of the JDK between two sibling interfaces, and it has to be respected:
 * a reader that throws instead of returning null cuts the search short.
 */
public abstract class SoundbankReader {

    /** For the subclasses. */
    protected SoundbankReader() {
    }

    /**
     * The bank at that URL, or null if it does not recognize it. See the class note.
     *
     * @throws InvalidMidiDataException if it recognizes it and it is broken
     * @throws IOException if it could not be read
     */
    public abstract Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException;

    /**
     * Likewise, from a stream.
     *
     * @throws InvalidMidiDataException if it recognizes it and it is broken
     * @throws IOException if it could not be read
     */
    public abstract Soundbank getSoundbank(InputStream stream)
        throws InvalidMidiDataException, IOException;

    /**
     * Likewise, from a file.
     *
     * @throws InvalidMidiDataException if it recognizes it and it is broken
     * @throws IOException if it could not be read
     */
    public abstract Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException;
}
