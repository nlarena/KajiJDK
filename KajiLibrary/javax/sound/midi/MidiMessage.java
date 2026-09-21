package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiMessage -- a raw MIDI message.
 *
 * <p>It keeps the bytes as they travel over the cable and nothing more. The three subclasses
 * interpret those bytes according to the standard's three families:
 *
 * <ul>
 *   <li>{@link ShortMessage}: one, two or three bytes. The notes, the controllers, the clock. It is
 *       ninety-nine per cent of the traffic;
 *   <li>{@link SysexMessage}: manufacturer-exclusive, of arbitrary length;
 *   <li>{@link MetaMessage}: does not exist on the cable. It only appears in files, and carries the
 *       tempo, the key signature, the track name.
 * </ul>
 *
 * <p>That last distinction matters and gets forgotten: sending a {@code MetaMessage} to a real
 * device makes no sense, because the 0xFF that heads it means "system reset" on the cable.
 *
 * <h2>The status byte</h2>
 *
 * <p>The first, and it always has the high bit set. The data bytes never have it, and that is why a
 * receiver can resynchronize in the middle of a stream: it finds the next byte greater than or
 * equal to 0x80 and knows a message starts there.
 *
 * <p>{@link #getStatus} returns it <b>unsigned</b>, from 0 to 255. It is the one to use: reading
 * {@code getMessage()[0]} gives a negative {@code byte} and comparing that against {@code 0x90}
 * fails.
 */
public abstract class MidiMessage implements Cloneable {

    /** The raw bytes; it can be longer than {@link #length}. */
    protected byte[] data;

    /** How many of those bytes count. */
    protected int length = 0;

    /** For the subclasses. */
    protected MidiMessage(byte[] data) {
        this.data = data;
        if (data == null) {
            this.length = 0;
        } else {
            this.length = data.length;
        }
    }

    /**
     * Replaces the bytes.
     *
     * @throws InvalidMidiDataException if the length is negative or larger than the array
     */
    protected void setMessage(byte[] data, int length) throws InvalidMidiDataException {
        if (length < 0 || (length > 0 && length > data.length)) {
            throw new InvalidMidiDataException("length out of bounds: " + length);
        }
        this.length = length;
        if (this.data == null || this.data.length < this.length) {
            this.data = new byte[this.length];
        }
        System.arraycopy(data, 0, this.data, 0, length);
    }

    /** A copy of the bytes that count. */
    public byte[] getMessage() {
        byte[] copy = new byte[this.length];
        if (this.data != null) {
            System.arraycopy(this.data, 0, copy, 0, this.length);
        }
        return copy;
    }

    /** The status byte, from 0 to 255. See the class note. */
    public int getStatus() {
        if (this.length > 0) {
            return this.data[0] & 0xFF;
        }
        return 0;
    }

    /** How many bytes it has. */
    public int getLength() {
        return this.length;
    }

    /** An independent copy, with its own array. */
    @Override
    public abstract Object clone();
}
