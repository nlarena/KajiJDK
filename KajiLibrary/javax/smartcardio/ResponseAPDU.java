package javax.smartcardio;

import java.io.Serializable;
import java.util.Arrays;

/**
 * KajiLibrary's javax.smartcardio.ResponseAPDU -- what the card answers.
 *
 * <p>An answer is the data --which may be zero bytes-- and always <b>two status bytes</b> at the
 * end, SW1 and SW2. That is why a valid answer never measures less than two bytes.
 *
 * <p>{@code 9000} is "all fine"; {@code 6A82} is "that file does not exist"; {@code 61xx} is "there
 * are xx more bytes waiting". {@link #getSW} returns both together, which is the form in which they
 * are tabulated in the standard.
 */
public final class ResponseAPDU implements Serializable {

    private static final long serialVersionUID = 6962744978375594225L;

    /** The whole answer, data plus status. */
    private final byte[] apdu;

    /**
     * With those bytes. The array is copied.
     *
     * @throws NullPointerException if it is null
     * @throws IllegalArgumentException if it measures less than two bytes
     */
    public ResponseAPDU(byte[] apdu) {
        apdu = apdu.clone();
        if (apdu.length < 2) {
            throw new IllegalArgumentException("apdu must be at least 2 bytes long");
        }
        this.apdu = apdu;
    }

    /** How many data bytes there are, without counting the status. */
    public int getNr() {
        return this.apdu.length - 2;
    }

    /** The data, without the status. A copy. */
    public byte[] getData() {
        byte[] data = new byte[this.apdu.length - 2];
        System.arraycopy(this.apdu, 0, data, 0, data.length);
        return data;
    }

    /** The first status byte. */
    public int getSW1() {
        return this.apdu[this.apdu.length - 2] & 0xFF;
    }

    /** The second. */
    public int getSW2() {
        return this.apdu[this.apdu.length - 1] & 0xFF;
    }

    /** Both together, SW1 on top. See the class note. */
    public int getSW() {
        return (getSW1() << 8) | getSW2();
    }

    /** The whole answer. A copy. */
    public byte[] getBytes() {
        return this.apdu.clone();
    }

    /** The size and the status in hexadecimal. */
    @Override
    public String toString() {
        return "ResponseAPDU: " + this.apdu.length + " bytes, SW="
            + Integer.toHexString(getSW() | 0x10000).substring(1);
    }

    /** Two answers are equal if they have the same bytes. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResponseAPDU)) {
            return false;
        }
        return Arrays.equals(this.apdu, ((ResponseAPDU) obj).apdu);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.apdu);
    }
}
