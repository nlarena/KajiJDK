package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.SysexMessage -- a manufacturer-exclusive message.
 *
 * <p>The standard's escape valve: whatever MIDI does not define, each manufacturer sends through
 * here. Loading a sound into a synthesizer, dumping its configuration, updating its firmware.
 *
 * <p>It starts with {@link #SYSTEM_EXCLUSIVE} (0xF0), goes on with the manufacturer's identifier
 * and whatever it wants, and ends with 0xF7. It is the only message of arbitrary length.
 *
 * <h2>The two status constants</h2>
 *
 * <p>{@link #SPECIAL_SYSTEM_EXCLUSIVE} is 0xF7 and it is not "the end": it is the status of a
 * <b>continuation</b>. A very long exclusive can be sent split, and the pieces that are not the
 * first carry that status.
 *
 * <p>It is the part that gets misread: receiving a message with status 0xF7 does not mean one
 * ended, it means a piece from the middle or the end arrived.
 *
 * <p>{@link #getData} returns everything but the status byte, so the closing 0xF7 <b>does</b> go
 * in.
 */
public class SysexMessage extends MidiMessage {

    /** The start of an exclusive. */
    public static final int SYSTEM_EXCLUSIVE = 0xF0;

    /** The continuation of a split one. See the class note. */
    public static final int SPECIAL_SYSTEM_EXCLUSIVE = 0xF7;

    /** An empty exclusive: {@code 0xF0 0xF7}. */
    public SysexMessage() {
        this(new byte[] { (byte) SYSTEM_EXCLUSIVE, (byte) SPECIAL_SYSTEM_EXCLUSIVE });
    }

    /**
     * An exclusive with those bytes, the status one included.
     *
     * @throws InvalidMidiDataException if the first byte is neither 0xF0 nor 0xF7
     */
    public SysexMessage(byte[] data, int length) throws InvalidMidiDataException {
        super(null);
        setMessage(data, length);
    }

    /**
     * Likewise, with the status separate.
     *
     * @throws InvalidMidiDataException if the status is neither 0xF0 nor 0xF7
     */
    public SysexMessage(int status, byte[] data, int length) throws InvalidMidiDataException {
        super(null);
        setMessage(status, data, length);
    }

    /** For the subclasses and the readers. */
    protected SysexMessage(byte[] data) {
        super(data);
    }

    /**
     * Replaces the bytes; the first has to be the status.
     *
     * @throws InvalidMidiDataException if the first byte is neither 0xF0 nor 0xF7
     */
    @Override
    public void setMessage(byte[] data, int length) throws InvalidMidiDataException {
        int status = 0;
        if (data != null && length > 0) {
            status = data[0] & 0xFF;
        }
        if (status != SYSTEM_EXCLUSIVE && status != SPECIAL_SYSTEM_EXCLUSIVE) {
            throw new InvalidMidiDataException("Invalid status byte for sysex message: 0x"
                + Integer.toHexString(status));
        }
        super.setMessage(data, length);
    }

    /**
     * Replaces the bytes, with the status separate.
     *
     * @throws InvalidMidiDataException if the status is neither 0xF0 nor 0xF7
     */
    public void setMessage(int status, byte[] data, int length) throws InvalidMidiDataException {
        if (status != SYSTEM_EXCLUSIVE && status != SPECIAL_SYSTEM_EXCLUSIVE) {
            throw new InvalidMidiDataException("Invalid status byte for sysex message: 0x"
                + Integer.toHexString(status));
        }
        if (length < 0 || (length > 0 && length > data.length)) {
            throw new InvalidMidiDataException("length out of bounds: " + length);
        }
        this.length = length + 1;
        this.data = new byte[this.length];
        this.data[0] = (byte) (status & 0xFF);
        if (length > 0) {
            System.arraycopy(data, 0, this.data, 1, length);
        }
    }

    /** A copy of everything but the status byte. See the class note. */
    public byte[] getData() {
        int n = this.length - 1;
        if (n < 0) {
            n = 0;
        }
        byte[] copy = new byte[n];
        System.arraycopy(this.data, 1, copy, 0, n);
        return copy;
    }

    /** An independent copy. */
    @Override
    public Object clone() {
        byte[] copy = new byte[this.length];
        System.arraycopy(this.data, 0, copy, 0, this.length);
        return new SysexMessage(copy);
    }
}
