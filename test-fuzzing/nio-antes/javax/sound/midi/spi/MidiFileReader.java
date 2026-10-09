package javax.sound.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.Sequence;

/**
 * KajiLibrary's javax.sound.midi.spi.MidiFileReader -- reads MIDI files of a format.
 *
 * <p>It is registered as a service and {@code MidiSystem} finds it by itself.
 *
 * <p>As with sampled audio, {@code MidiSystem} tries the readers one at a time on the same stream:
 * it has to be marked before looking and <b>rewound</b> if the format is not recognized. Not
 * recognizing it is signalled with {@link InvalidMidiDataException}, which {@code MidiSystem}
 * catches to go on.
 *
 * <p>The six methods are two times three: the header or the whole piece, from a stream, a URL or a
 * file.
 */
public abstract class MidiFileReader {

    /** For the subclasses. */
    protected MidiFileReader() {
    }

    /**
     * What there is in that stream, without reading the piece.
     *
     * <p>It has to leave it as it found it; see the class note.
     *
     * @throws InvalidMidiDataException if this reader does not recognize it
     * @throws IOException if it could not be read
     */
    public abstract MidiFileFormat getMidiFileFormat(InputStream stream)
        throws InvalidMidiDataException, IOException;

    /**
     * Likewise, from a URL.
     *
     * @throws InvalidMidiDataException if this reader does not recognize it
     * @throws IOException if it could not be read
     */
    public abstract MidiFileFormat getMidiFileFormat(URL url)
        throws InvalidMidiDataException, IOException;

    /**
     * Likewise, from a file.
     *
     * @throws InvalidMidiDataException if this reader does not recognize it
     * @throws IOException if it could not be read
     */
    public abstract MidiFileFormat getMidiFileFormat(File file)
        throws InvalidMidiDataException, IOException;

    /**
     * The whole piece.
     *
     * @throws InvalidMidiDataException if this reader does not recognize it
     * @throws IOException if it could not be read
     */
    public abstract Sequence getSequence(InputStream stream)
        throws InvalidMidiDataException, IOException;

    /**
     * Likewise, from a URL.
     *
     * @throws InvalidMidiDataException if this reader does not recognize it
     * @throws IOException if it could not be read
     */
    public abstract Sequence getSequence(URL url) throws InvalidMidiDataException, IOException;

    /**
     * Likewise, from a file.
     *
     * @throws InvalidMidiDataException if this reader does not recognize it
     * @throws IOException if it could not be read
     */
    public abstract Sequence getSequence(File file) throws InvalidMidiDataException, IOException;
}
