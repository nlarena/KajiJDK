package javax.smartcardio;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * KajiLibrary's javax.smartcardio.CommandAPDU -- a command for the card.
 *
 * <p>Every command starts with four bytes: class, instruction and two parameters. After that it may
 * carry data and may ask for an answer of a certain size, and from those two options come the four
 * cases of the ISO 7816-4 standard:
 *
 * <table border="1">
 * <caption>The four cases</caption>
 * <tr><th>case</th><th>data (Nc)</th><th>answer (Ne)</th><th>length</th></tr>
 * <tr><td>1</td><td>no</td><td>no</td><td>4</td></tr>
 * <tr><td>2</td><td>no</td><td>yes</td><td>5 or 7</td></tr>
 * <tr><td>3</td><td>yes</td><td>no</td><td>4 + 1 + Nc, or 4 + 3 + Nc</td></tr>
 * <tr><td>4</td><td>yes</td><td>yes</td><td>4 + 1 + Nc + 1, or 4 + 3 + Nc + 2</td></tr>
 * </table>
 *
 * <h2>Short and extended</h2>
 *
 * <p>One byte is not enough to say 300, so the standard has two encodings. In the short one the
 * length goes in one byte; in the extended one it goes in three --a zero and then two bytes-- and
 * the whole command changes shape.
 *
 * <p>The zero rule is the one to keep in mind: a length byte of zero does <b>not</b> mean zero. As
 * Le it means 256, and as the mark at the start of an extended length it means that two more bytes
 * follow. A literal zero cannot be written, and that is why asking for Ne equal to zero is the same
 * as asking for no answer.
 *
 * <p>This class chooses the encoding by itself: it uses the short one while it fits, and moves to
 * the extended one when the data goes past 255 bytes or the answer asked for goes past 256. When
 * built from bytes already put together, on the other hand, {@link #getBytes} returns exactly the
 * ones it was given, without re-encoding.
 */
public final class CommandAPDU implements Serializable {

    private static final long serialVersionUID = 398698301286670877L;

    /** The maximum that fits in the short encoding of the data. */
    private static final int MAX_APDU_SIZE_SHORT = 255;

    /** The whole command, as it goes to the reader. */
    private byte[] apdu;

    /** Where the data starts within {@link #apdu}. */
    private transient int dataOffset;

    /** How many data bytes it carries. */
    private transient int nc;

    /** How many answer bytes it asks for. */
    private transient int ne;

    /**
     * From a command already put together. The bytes are copied and interpreted to get Nc and Ne.
     *
     * @throws NullPointerException if it is null
     * @throws IllegalArgumentException if it measures less than four bytes or is none of the four
     *     cases
     */
    public CommandAPDU(byte[] apdu) {
        this.apdu = apdu.clone();
        parse();
    }

    /**
     * From a piece of an array.
     *
     * @throws IndexOutOfBoundsException if the piece goes outside the array
     */
    public CommandAPDU(byte[] apdu, int apduOffset, int apduLength) {
        checkRange(apdu.length, apduOffset, apduLength);
        this.apdu = new byte[apduLength];
        System.arraycopy(apdu, apduOffset, this.apdu, 0, apduLength);
        parse();
    }

    /**
     * From whatever is left to read in a buffer.
     *
     * <p>It consumes the buffer: on return, its position is at the limit.
     */
    public CommandAPDU(ByteBuffer apdu) {
        this.apdu = new byte[apdu.remaining()];
        apdu.get(this.apdu);
        parse();
    }

    /** Case 1: no data and no answer. */
    public CommandAPDU(int cla, int ins, int p1, int p2) {
        this(cla, ins, p1, p2, null, 0, 0, 0);
    }

    /** Case 2: asks for {@code ne} answer bytes. */
    public CommandAPDU(int cla, int ins, int p1, int p2, int ne) {
        this(cla, ins, p1, p2, null, 0, 0, ne);
    }

    /** Case 3: sends data and expects no answer. */
    public CommandAPDU(int cla, int ins, int p1, int p2, byte[] data) {
        this(cla, ins, p1, p2, data, 0, arrayLength(data), 0);
    }

    /** Case 3, with the data in a piece of the array. */
    public CommandAPDU(int cla, int ins, int p1, int p2, byte[] data, int dataOffset,
                       int dataLength) {
        this(cla, ins, p1, p2, data, dataOffset, dataLength, 0);
    }

    /** Case 4: sends data and asks for an answer. */
    public CommandAPDU(int cla, int ins, int p1, int p2, byte[] data, int ne) {
        this(cla, ins, p1, p2, data, 0, arrayLength(data), ne);
    }

    /**
     * Case 4, with the data in a piece of the array. It is the constructor all the others end up
     * in.
     *
     * @throws IllegalArgumentException if {@code ne} is negative or goes past 65536, or if the data
     *     goes past 65535 bytes
     * @throws IndexOutOfBoundsException if the piece goes outside the array
     */
    public CommandAPDU(int cla, int ins, int p1, int p2, byte[] data, int dataOffset,
                       int dataLength, int ne) {
        if (dataLength < 0) {
            throw new IllegalArgumentException("dataLength must not be negative");
        }
        if (ne < 0) {
            throw new IllegalArgumentException("ne must not be negative");
        }
        if (ne > 65536) {
            throw new IllegalArgumentException("ne is too large");
        }
        if (dataLength > 65535) {
            throw new IllegalArgumentException("dataLength is too large");
        }
        if (data != null) {
            checkRange(data.length, dataOffset, dataLength);
        } else if (dataLength != 0) {
            throw new IllegalArgumentException("dataLength must be 0 if data is null");
        }
        this.nc = dataLength;
        this.ne = ne;
        build(cla, ins, p1, p2, data, dataOffset);
    }

    /** Puts the bytes together choosing the encoding. See the class note. */
    private void build(int cla, int ins, int p1, int p2, byte[] data, int dataOffset) {
        boolean extended = this.nc > MAX_APDU_SIZE_SHORT || this.ne > 256;
        int length = 4;
        if (this.nc > 0) {
            length = length + (extended ? 3 : 1) + this.nc;
        }
        if (this.ne > 0) {
            if (this.nc > 0) {
                length = length + (extended ? 2 : 1);
            } else {
                length = length + (extended ? 3 : 1);
            }
        }
        this.apdu = new byte[length];
        this.apdu[0] = (byte) cla;
        this.apdu[1] = (byte) ins;
        this.apdu[2] = (byte) p1;
        this.apdu[3] = (byte) p2;
        int at = 4;
        if (this.nc > 0) {
            if (extended) {
                this.apdu[at] = 0;
                this.apdu[at + 1] = (byte) (this.nc >> 8);
                this.apdu[at + 2] = (byte) this.nc;
                at = at + 3;
            } else {
                this.apdu[at] = (byte) this.nc;
                at = at + 1;
            }
            this.dataOffset = at;
            System.arraycopy(data, dataOffset, this.apdu, at, this.nc);
            at = at + this.nc;
        } else {
            this.dataOffset = at;
        }
        if (this.ne > 0) {
            if (extended) {
                if (this.nc == 0) {
                    this.apdu[at] = 0;
                    at = at + 1;
                }
                // 65536 is written as two zeros: the literal zero is not needed, because asking for
                // zero bytes is the same as asking for nothing.
                this.apdu[at] = (byte) (this.ne >> 8);
                this.apdu[at + 1] = (byte) this.ne;
            } else {
                this.apdu[at] = (byte) this.ne;
            }
        }
    }

    /** Interprets some bytes already put together to get Nc and Ne. See the class note. */
    private void parse() {
        if (this.apdu.length < 4) {
            throw new IllegalArgumentException("apdu must be at least 4 bytes long");
        }
        if (this.apdu.length == 4) {
            this.nc = 0;
            this.ne = 0;
            this.dataOffset = 4;
            return;
        }
        int first = this.apdu[4] & 0xFF;
        if (this.apdu.length == 5) {
            this.nc = 0;
            this.dataOffset = 5;
            this.ne = first == 0 ? 256 : first;
            return;
        }
        if (first != 0) {
            if (this.apdu.length == 4 + 1 + first) {
                this.nc = first;
                this.ne = 0;
                this.dataOffset = 5;
                return;
            }
            if (this.apdu.length == 4 + 2 + first) {
                this.nc = first;
                this.dataOffset = 5;
                int le = this.apdu[this.apdu.length - 1] & 0xFF;
                this.ne = le == 0 ? 256 : le;
                return;
            }
            throw new IllegalArgumentException("Invalid APDU: length=" + this.apdu.length
                + ", b1=" + first);
        }
        if (this.apdu.length < 7) {
            throw new IllegalArgumentException("Invalid APDU: length=" + this.apdu.length
                + ", b1=" + first);
        }
        int extended = ((this.apdu[5] & 0xFF) << 8) | (this.apdu[6] & 0xFF);
        if (this.apdu.length == 7) {
            this.nc = 0;
            this.dataOffset = 7;
            this.ne = extended == 0 ? 65536 : extended;
            return;
        }
        if (extended == 0) {
            throw new IllegalArgumentException("Invalid APDU: length=" + this.apdu.length
                + ", b1=" + first);
        }
        if (this.apdu.length == 7 + extended) {
            this.nc = extended;
            this.ne = 0;
            this.dataOffset = 7;
            return;
        }
        if (this.apdu.length == 9 + extended) {
            this.nc = extended;
            this.dataOffset = 7;
            int le = ((this.apdu[this.apdu.length - 2] & 0xFF) << 8)
                | (this.apdu[this.apdu.length - 1] & 0xFF);
            this.ne = le == 0 ? 65536 : le;
            return;
        }
        throw new IllegalArgumentException("Invalid APDU: length=" + this.apdu.length
            + ", b1=" + first);
    }

    /** The class byte. */
    public int getCLA() {
        return this.apdu[0] & 0xFF;
    }

    /** The instruction byte. */
    public int getINS() {
        return this.apdu[1] & 0xFF;
    }

    /** The first parameter. */
    public int getP1() {
        return this.apdu[2] & 0xFF;
    }

    /** The second. */
    public int getP2() {
        return this.apdu[3] & 0xFF;
    }

    /** How many data bytes it carries. */
    public int getNc() {
        return this.nc;
    }

    /** The data. A copy; empty if it carries none. */
    public byte[] getData() {
        byte[] data = new byte[this.nc];
        System.arraycopy(this.apdu, this.dataOffset, data, 0, this.nc);
        return data;
    }

    /** How many answer bytes it asks for; zero if it asks for none. */
    public int getNe() {
        return this.ne;
    }

    /** The whole command. A copy. */
    public byte[] getBytes() {
        return this.apdu.clone();
    }

    /** The size, Nc and Ne. */
    @Override
    public String toString() {
        // The JDK writes "CommmandAPDU" with three m's; it is copied as it is so that a program
        // that compares this output keeps seeing the same.
        return "CommmandAPDU: " + this.apdu.length + " bytes, nc=" + this.nc + ", ne=" + this.ne;
    }

    /** Two commands are equal if they have the same bytes. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CommandAPDU)) {
            return false;
        }
        return Arrays.equals(this.apdu, ((CommandAPDU) obj).apdu);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.apdu);
    }

    /** The length of the array, or zero if it is null. */
    private static int arrayLength(byte[] data) {
        return data == null ? 0 : data.length;
    }

    /** That the piece fits in the array. */
    private static void checkRange(int arrayLength, int offset, int length) {
        if ((offset < 0) || (length < 0) || (offset > arrayLength - length)) {
            throw new IllegalArgumentException("Offset or length invalid");
        }
    }

    /**
     * When read from a stream the bytes have to be interpreted again: Nc and Ne are not serialised.
     */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        parse();
    }
}
