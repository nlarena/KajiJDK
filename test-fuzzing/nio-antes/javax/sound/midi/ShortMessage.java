package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.ShortMessage -- a MIDI message of one to three bytes.
 *
 * <p>Almost all of MIDI is this: notes, controllers, program change, pitch wheel, and the clock
 * messages.
 *
 * <h2>Command and channel</h2>
 *
 * <p>The status byte carries both things: the four high bits are the <b>command</b> and the four
 * low ones the <b>channel</b>. That is why there are sixteen channels and not seventeen.
 *
 * <p>The trap: in the system messages --0xF0 and up-- that division does not exist, the whole byte
 * is the command. {@link #getChannel} still returns the four low bits, and there the number means
 * nothing. The command has to be looked at first.
 *
 * <h2>A note is turned off with zero volume</h2>
 *
 * <p>{@link #NOTE_ON} with {@code data2} at 0 is equivalent to {@link #NOTE_OFF}. It is an
 * optimization of the standard --it allows sending a string of notes without repeating the status
 * byte-- and everything that processes MIDI has to account for it. Handling only {@code NOTE_OFF}
 * leaves notes hanging, sounding forever.
 */
public class ShortMessage extends MidiMessage {

    /** Timecode quarter frame. */
    public static final int MIDI_TIME_CODE = 0xF1;

    /** Position in the song. */
    public static final int SONG_POSITION_POINTER = 0xF2;

    /** Song select. */
    public static final int SONG_SELECT = 0xF3;

    /** Tune request. */
    public static final int TUNE_REQUEST = 0xF6;

    /** End of an exclusive message. */
    public static final int END_OF_EXCLUSIVE = 0xF7;

    /** Clock tick; there are twenty-four per quarter note. */
    public static final int TIMING_CLOCK = 0xF8;

    /** Start playing. */
    public static final int START = 0xFA;

    /** Continue from where it stopped. */
    public static final int CONTINUE = 0xFB;

    /** Stop. */
    public static final int STOP = 0xFC;

    /** Active sensing; says the cable is still alive. */
    public static final int ACTIVE_SENSING = 0xFE;

    /** System reset. */
    public static final int SYSTEM_RESET = 0xFF;

    /** Release a note. */
    public static final int NOTE_OFF = 0x80;

    /** Play a note. See the class note on zero volume. */
    public static final int NOTE_ON = 0x90;

    /** Pressure on a key already pressed. */
    public static final int POLY_PRESSURE = 0xA0;

    /** Move a controller. */
    public static final int CONTROL_CHANGE = 0xB0;

    /** Change sound. */
    public static final int PROGRAM_CHANGE = 0xC0;

    /** Pressure on the whole channel. */
    public static final int CHANNEL_PRESSURE = 0xD0;

    /** Pitch wheel. */
    public static final int PITCH_BEND = 0xE0;

    /**
     * A middle note at maximum volume.
     *
     * <p>The bytes are {@code 0x90 0x40 0x7F}. It is an arbitrary value the JDK chose so that a
     * newly built message is valid and not empty.
     */
    public ShortMessage() {
        this(new byte[3]);
        try {
            setMessage(NOTE_ON, 64, 127);
        } catch (InvalidMidiDataException e) {
            // The values are constant and valid; it cannot happen.
        }
    }

    /**
     * A system message without data.
     *
     * @throws InvalidMidiDataException if that status carries data
     */
    public ShortMessage(int status) throws InvalidMidiDataException {
        this(new byte[3]);
        setMessage(status);
    }

    /**
     * A channel message with two data bytes.
     *
     * @throws InvalidMidiDataException if anything is out of range
     */
    public ShortMessage(int status, int data1, int data2) throws InvalidMidiDataException {
        this(new byte[3]);
        setMessage(status, data1, data2);
    }

    /**
     * Likewise, with the channel separate.
     *
     * @param command the command, without the channel
     * @throws InvalidMidiDataException if anything is out of range
     */
    public ShortMessage(int command, int channel, int data1, int data2)
        throws InvalidMidiDataException {
        this(new byte[3]);
        setMessage(command, channel, data1, data2);
    }

    /** For the subclasses and the readers. */
    protected ShortMessage(byte[] data) {
        super(data);
    }

    /**
     * Sets a message without data.
     *
     * @throws InvalidMidiDataException if that status carries data
     */
    public void setMessage(int status) throws InvalidMidiDataException {
        int dataLength = getDataLength(status);
        if (dataLength != 0) {
            throw new InvalidMidiDataException("Status byte; " + status + " requires "
                + dataLength + " data bytes");
        }
        setMessage(status, 0, 0);
    }

    /**
     * Sets a message with up to two data bytes.
     *
     * <p>The data bytes that status does not use are ignored, but they are still validated.
     *
     * @throws InvalidMidiDataException if the status or the data are out of range
     */
    public void setMessage(int status, int data1, int data2) throws InvalidMidiDataException {
        int dataLength = getDataLength(status);
        if (dataLength > 0) {
            if (data1 < 0 || data1 > 127) {
                throw new InvalidMidiDataException("data1 out of range: " + data1);
            }
            if (dataLength > 1 && (data2 < 0 || data2 > 127)) {
                throw new InvalidMidiDataException("data2 out of range: " + data2);
            }
        }
        this.length = dataLength + 1;
        if (this.data == null || this.data.length < this.length) {
            this.data = new byte[3];
        }
        this.data[0] = (byte) (status & 0xFF);
        if (this.length > 1) {
            this.data[1] = (byte) (data1 & 0xFF);
            if (this.length > 2) {
                this.data[2] = (byte) (data2 & 0xFF);
            }
        }
    }

    /**
     * Sets a channel message, with the command and the channel separately.
     *
     * @throws InvalidMidiDataException if the command, the channel or the data are out of range
     */
    public void setMessage(int command, int channel, int data1, int data2)
        throws InvalidMidiDataException {
        if (command >= 0xF0 || command < 0x80) {
            throw new InvalidMidiDataException("command out of range: 0x"
                + Integer.toHexString(command));
        }
        if ((channel & 0xFFFFFFF0) != 0) {
            throw new InvalidMidiDataException("channel out of range: " + channel);
        }
        setMessage((command & 0xF0) | (channel & 0x0F), data1, data2);
    }

    /**
     * The four low bits of the status.
     *
     * <p>It only means something in channel messages; see the class note.
     */
    public int getChannel() {
        return getStatus() & 0x0F;
    }

    /** The four high bits of the status. */
    public int getCommand() {
        return getStatus() & 0xF0;
    }

    /** The first data byte, or 0 if there is none. */
    public int getData1() {
        if (this.length > 1) {
            return this.data[1] & 0xFF;
        }
        return 0;
    }

    /** The second data byte, or 0 if there is none. */
    public int getData2() {
        if (this.length > 2) {
            return this.data[2] & 0xFF;
        }
        return 0;
    }

    /** An independent copy. */
    @Override
    public Object clone() {
        byte[] copy = new byte[this.length];
        System.arraycopy(this.data, 0, copy, 0, this.length);
        ShortMessage msg = new ShortMessage(copy);
        return msg;
    }

    /**
     * How many data bytes that status carries.
     *
     * <p>Channel messages follow the usual pattern: two data bytes, except program change and
     * channel pressure, which carry one. System messages have no pattern and have to be known by
     * heart.
     *
     * <p>{@code 0xF0} --the start of an exclusive-- throws an exception: it has no fixed length,
     * and that is why it is not a {@code ShortMessage}.
     *
     * @throws InvalidMidiDataException if that byte is not a valid status for this class
     */
    protected final int getDataLength(int status) throws InvalidMidiDataException {
        int cmd = status & 0xF0;
        if (cmd == NOTE_OFF || cmd == NOTE_ON || cmd == POLY_PRESSURE
            || cmd == CONTROL_CHANGE || cmd == PITCH_BEND) {
            return 2;
        }
        if (cmd == PROGRAM_CHANGE || cmd == CHANNEL_PRESSURE) {
            return 1;
        }
        if (cmd == 0xF0) {
            if (status == MIDI_TIME_CODE || status == SONG_SELECT) {
                return 1;
            }
            if (status == SONG_POSITION_POINTER) {
                return 2;
            }
            if (status == 0xF0 || status == 0xF4 || status == 0xF5) {
                throw new InvalidMidiDataException("Invalid status byte: " + status);
            }
            return 0;
        }
        throw new InvalidMidiDataException("Invalid status byte: " + status);
    }
}
