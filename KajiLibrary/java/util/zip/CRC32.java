package java.util.zip;

// CRC-32 as used by zip, gzip and PNG (the IEEE 802.3 polynomial). A CRC is division: the
// message is read as one huge binary polynomial and the checksum is the REMAINDER of dividing it
// by a fixed generator. Doing that bit by bit is a shift and a conditional xor; the table below
// pre-computes what eight of those steps do at once, which is why the loop handles a whole byte
// per iteration.
public class CRC32 implements Checksum {

    // The generator polynomial, bit-reversed. `0xEDB88320` is the reflection of the usual
    // `0x04C11DB7`, and the reflection is what lets the algorithm shift RIGHT — matching the
    // order the bits arrive in on the wire.
    private static final int POLY = 0xEDB88320;

    private static final int[] TABLE = buildTable();

    // One entry per possible byte: the remainder that byte alone would leave.
    private static int[] buildTable() {
        int[] table = new int[256];
        int n = 0;
        while (n < 256) {
            int c = n;
            int k = 0;
            while (k < 8) {
                // The low bit decides: if it is set, subtract (xor) the polynomial.
                if ((c & 1) != 0) {
                    c = POLY ^ (c >>> 1);
                } else {
                    c = c >>> 1;
                }
                k = k + 1;
            }
            table[n] = c;
            n = n + 1;
        }
        return table;
    }

    // Held inverted (the register is complemented on the way in and out) — the standard trick
    // that makes leading zero bytes actually change the result.
    private int crc;

    public CRC32() {
        reset();
    }

    public void update(int value) {
        crc = TABLE[(crc ^ value) & 0xff] ^ (crc >>> 8);
    }

    public void update(byte[] buf, int off, int len) {
        int i = off;
        int end = off + len;
        while (i < end) {
            crc = TABLE[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
            i = i + 1;
        }
    }

    public void reset() {
        crc = 0xFFFFFFFF;
    }

    public long getValue() {
        // Complement on the way out, then widen without sign extension.
        return (long) (crc ^ 0xFFFFFFFF) & 0xFFFFFFFFL;
    }
}
