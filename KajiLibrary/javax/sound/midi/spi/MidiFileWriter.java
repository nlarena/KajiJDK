package javax.sound.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import javax.sound.midi.Sequence;

/**
 * KajiLibrary's javax.sound.midi.spi.MidiFileWriter -- writes MIDI files.
 *
 * <p>The mirror of {@link MidiFileReader}. The types are numbers --0, 1 or 2-- and not objects; see
 * {@code MidiFileFormat} on what each one means.
 *
 * <p>The two {@code getMidiFileTypes} have the same distinction as in sampled audio: the one
 * without arguments says what it can write in general, the one that takes a piece says what it can
 * write <b>with that one</b>. A multi-track piece does not fit in a type 0 file without mixing the
 * tracks, and there are implementations that refuse to mix.
 */
public abstract class MidiFileWriter {

    /** For the subclasses. */
    protected MidiFileWriter() {
    }

    /** Which types it can write in general. */
    public abstract int[] getMidiFileTypes();

    /** Which types it can write with that piece. See the class note. */
    public abstract int[] getMidiFileTypes(Sequence sequence);

    /** Whether it can write that type. */
    public boolean isFileTypeSupported(int fileType) {
        return contains(getMidiFileTypes(), fileType);
    }

    /** Whether it can write that type with that piece. */
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
        return contains(getMidiFileTypes(sequence), fileType);
    }

    /**
     * Writes.
     *
     * @return how many bytes were written
     * @throws IOException if it could not be written
     * @throws IllegalArgumentException if it does not support that type with that piece
     */
    public abstract int write(Sequence in, int fileType, OutputStream out) throws IOException;

    /**
     * Likewise, to a file.
     *
     * @return how many bytes were written
     * @throws IOException if it could not be written
     * @throws IllegalArgumentException if it does not support that type with that piece
     */
    public abstract int write(Sequence in, int fileType, File out) throws IOException;

    /** Whether that value is in the array. */
    private static boolean contains(int[] all, int one) {
        int i = 0;
        while (all != null && i < all.length) {
            if (all[i] == one) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
