package java.awt.image;

/**
 * A {@link LookupTable} of bytes.
 *
 * <p>The values are read **unsigned**, as in {@link DataBufferByte}: an entry 0xFF is worth 255.
 * Without that, the light half of the table would give negative values.
 */
public class ByteLookupTable extends LookupTable {

    private final byte[][] data;

    /**
     * One table per component.
     *
     * <p>The arrays **are not copied**: the table keeps the ones it is given, and only the array of
     * arrays is its own. It is what the JDK does and what allows a big table to be shared between
     * several filters without duplicating it.
     *
     * @throws IllegalArgumentException if the offset is negative
     */
    public ByteLookupTable(int offset, byte[][] data) {
        super(offset, data.length);
        this.data = new byte[data.length][];
        for (int i = 0; i < data.length; i++) {
            this.data[i] = data[i];
        }
    }

    /** A single table, which is applied to every component. */
    public ByteLookupTable(int offset, byte[] data) {
        super(offset, 1);
        this.data = new byte[1][];
        this.data[0] = data;
    }

    /** The tables, without copying. */
    public final byte[][] getTable() {
        return this.data;
    }

    /**
     * Applies the table to a pixel.
     *
     * <p>With a single table that one is used for every component; with several, the one that
     * corresponds to each.
     *
     * @throws ArrayIndexOutOfBoundsException if a value falls outside the table after subtracting
     *     the offset from it
     */
    public int[] lookupPixel(int[] src, int[] dst) {
        int[] out = dst == null ? new int[src.length] : dst;
        if (this.data.length == 1) {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[0][src[i] - this.getOffset()] & 0xFF;
            }
        } else {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[i][src[i] - this.getOffset()] & 0xFF;
            }
        }
        return out;
    }

    /** The same as the other form, with the type of this table. */
    public byte[] lookupPixel(byte[] src, byte[] dst) {
        byte[] out = dst == null ? new byte[src.length] : dst;
        if (this.data.length == 1) {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[0][(src[i] & 0xFF) - this.getOffset()];
            }
        } else {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[i][(src[i] & 0xFF) - this.getOffset()];
            }
        }
        return out;
    }
}
