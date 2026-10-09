package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MetaMessage -- a message that only exists in files.
 *
 * <p>The tempo, the key signature, the time signature, the track name, the lyrics. None of this
 * travels over a MIDI cable: they are annotations of the file.
 *
 * <p>The status byte is {@link #META}, which is 0xFF -- the same as "system reset" on the cable.
 * That is why sending one of these to a real device not only does not work: it resets it.
 *
 * <h2>The length is encoded</h2>
 *
 * <p>The bytes are {@code 0xFF}, the type, the <b>length of the data as a variable-length
 * quantity</b>, and the data. That encoding --seven bits per byte, the high bit says more follows--
 * is the one the whole MIDI file format uses, and it is the reason a three-minute MIDI file takes a
 * few kilobytes.
 *
 * <p>{@link #getData} returns only the data, without the header. {@link #getLength} returns the
 * total, header included, so the two do not match and need not.
 *
 * <h2>Type 0x2F is the end of track</h2>
 *
 * <p>It is mandatory and goes at the end of each track. {@link Track} keeps it by itself; it need
 * not be added by hand.
 */
public class MetaMessage extends MidiMessage {

    /** The status byte of all meta messages. */
    public static final int META = 0xFF;

    /** How many bytes the header takes: 0xFF, the type, and the variable length. */
    private int dataLength = 0;

    /** A meta message of type 0 without data. */
    public MetaMessage() {
        this(new byte[] { (byte) META, 0 });
    }

    /**
     * A meta message with type and data.
     *
     * @param type from 0 to 127
     * @throws InvalidMidiDataException if the type is out of range
     */
    public MetaMessage(int type, byte[] data, int length) throws InvalidMidiDataException {
        super(null);
        setMessage(type, data, length);
    }

    /**
     * For the subclasses and the readers.
     *
     * <p>It reads the header's variable length to know where the data starts; subtracting three is
     * not enough, because the length can take more than one byte.
     */
    protected MetaMessage(byte[] data) {
        super(data);
        this.dataLength = 0;
        if (data != null && data.length >= 3) {
            int at = 2;
            while (at < data.length && (data[at] & 0x80) != 0) {
                at = at + 1;
            }
            this.dataLength = data.length - (at + 1);
            if (this.dataLength < 0) {
                this.dataLength = 0;
            }
        }
    }

    /**
     * Replaces type and data.
     *
     * @param type from 0 to 127
     * @param length how many bytes of {@code data} to use
     * @throws InvalidMidiDataException if the type is out of range or the length does not add up
     */
    public void setMessage(int type, byte[] data, int length) throws InvalidMidiDataException {
        if (type >= 128 || type < 0) {
            throw new InvalidMidiDataException("Invalid meta event with type " + type);
        }
        if (length > 0 && (data == null || length > data.length)) {
            throw new InvalidMidiDataException("length out of bounds: " + length);
        }
        if (length < 0) {
            throw new InvalidMidiDataException("length out of bounds: " + length);
        }
        byte[] lengthBytes = variableLength(length);
        this.dataLength = length;
        this.length = 2 + lengthBytes.length + length;
        this.data = new byte[this.length];
        this.data[0] = (byte) META;
        this.data[1] = (byte) type;
        System.arraycopy(lengthBytes, 0, this.data, 2, lengthBytes.length);
        if (length > 0) {
            System.arraycopy(data, 0, this.data, 2 + lengthBytes.length, length);
        }
    }

    /** The type, from 0 to 127. */
    public int getType() {
        if (this.length >= 2) {
            return this.data[1] & 0xFF;
        }
        return 0;
    }

    /** A copy of the data, without the header. See the class note. */
    public byte[] getData() {
        byte[] copy = new byte[this.dataLength];
        System.arraycopy(this.data, this.length - this.dataLength, copy, 0, this.dataLength);
        return copy;
    }

    /** An independent copy. */
    @Override
    public Object clone() {
        byte[] copy = new byte[this.length];
        System.arraycopy(this.data, 0, copy, 0, this.length);
        return new MetaMessage(copy);
    }

    /**
     * Encodes a number as a variable-length quantity.
     *
     * <p>Seven bits per byte, from the most significant to the least; all but the last carry the
     * high bit set. See the class note.
     */
    private static byte[] variableLength(int value) {
        int bytes = 1;
        int probe = value >> 7;
        while (probe > 0) {
            bytes = bytes + 1;
            probe = probe >> 7;
        }
        byte[] out = new byte[bytes];
        int i = bytes - 1;
        out[i] = (byte) (value & 0x7F);
        int rest = value >> 7;
        while (i > 0) {
            i = i - 1;
            out[i] = (byte) ((rest & 0x7F) | 0x80);
            rest = rest >> 7;
        }
        return out;
    }
}
