package java.util.zip;

// CRC-32C — the same construction as `CRC32` with a different generator, Castagnoli's. The
// polynomial is not a cosmetic change: it detects more of the error patterns that matter at
// large block sizes, which is why storage and network formats reach for this one, and it is the
// variant modern CPUs implement in a single instruction.
public final class CRC32C implements Checksum {

    // Castagnoli's polynomial, bit-reversed — `0x1EDC6F41` reflected. See `CRC32` for why the
    // reflected form is the one that shifts right.
    private static final int POLY = 0x82F63B78;

    private static final int[] TABLE = buildTable();

    private static int[] buildTable() {
        int[] table = new int[256];
        int n = 0;
        while (n < 256) {
            int c = n;
            int k = 0;
            while (k < 8) {
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

    private int crc;

    public CRC32C() {
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
        return (long) (crc ^ 0xFFFFFFFF) & 0xFFFFFFFFL;
    }
}
