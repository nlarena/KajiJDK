package javax.smartcardio;

import java.io.Serializable;
import java.util.Arrays;

/**
 * KajiLibrary's javax.smartcardio.ATR -- the first thing a card says when it powers up.
 *
 * <p>ATR is <i>Answer To Reset</i>: the card answers with a string of bytes declaring at what speed
 * it talks, which protocols it understands and who it is. It is the only thing that can be read
 * without having negotiated anything.
 *
 * <h2>The historical bytes</h2>
 *
 * <p>{@link #getHistoricalBytes} returns the part at the end, which is the one that identifies the
 * product --the card's operating system, the issuer--. Getting to them is not cutting at a fixed
 * position: the ATR has to be <b>walked</b>, because the interface bytes before them are optional
 * and each one announces the next.
 *
 * <p>The T0 byte says how many historical bytes there are (the low four bits) and which of TA1,
 * TB1, TC1 and TD1 are present (the high four). If TD1 is there, its high four bits announce TA2,
 * TB2, TC2 and TD2, and so on until a TD does not announce the next one. A truncated ATR returns an
 * empty array instead of blowing up: what was read from a card pulled out halfway is not a
 * programming error.
 */
public final class ATR implements Serializable {

    private static final long serialVersionUID = 6695383790847736493L;

    /** The whole ATR, as it came. */
    private final byte[] atr;

    /** Where the historical bytes start, or -1 if the ATR is truncated. */
    private transient int startHistorical;

    /** How many historical bytes there are. */
    private transient int nHistorical;

    /**
     * With those bytes. The array is copied.
     *
     * @throws NullPointerException if it is null
     */
    public ATR(byte[] atr) {
        this.atr = atr.clone();
        parse();
    }

    /** Walks the interface bytes until it reaches the historical ones. See the class note. */
    private void parse() {
        this.startHistorical = -1;
        this.nHistorical = 0;
        if (this.atr.length < 2) {
            return;
        }
        int t0 = this.atr[1] & 0xFF;
        int count = t0 & 0x0F;
        int present = t0 >> 4;
        int at = 2;
        while (true) {
            int i = 0;
            while (i < 3) {
                if ((present & (1 << i)) != 0) {
                    at = at + 1;
                }
                i = i + 1;
            }
            if ((present & 8) == 0) {
                break;
            }
            if (at >= this.atr.length) {
                return;
            }
            present = (this.atr[at] & 0xFF) >> 4;
            at = at + 1;
        }
        if (at + count > this.atr.length) {
            return;
        }
        this.startHistorical = at;
        this.nHistorical = count;
    }

    /** The whole ATR. A copy. */
    public byte[] getBytes() {
        return this.atr.clone();
    }

    /**
     * The historical bytes. See the class note.
     *
     * @return an empty array if the ATR is truncated
     */
    public byte[] getHistoricalBytes() {
        if (this.startHistorical < 0) {
            return new byte[0];
        }
        byte[] result = new byte[this.nHistorical];
        System.arraycopy(this.atr, this.startHistorical, result, 0, this.nHistorical);
        return result;
    }

    /** How many bytes it has. */
    @Override
    public String toString() {
        return "ATR: " + this.atr.length + " bytes";
    }

    /** Two ATRs are equal if they have the same bytes. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ATR)) {
            return false;
        }
        return Arrays.equals(this.atr, ((ATR) obj).atr);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.atr);
    }

    /** When read from a stream the ATR has to be walked again: the position is not serialised. */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        parse();
    }
}
