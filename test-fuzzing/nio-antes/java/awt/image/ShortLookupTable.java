package java.awt.image;

/**
 * A {@link LookupTable} of shorts, read **unsigned** (0..65535).
 *
 * <p>It is the one that serves for images of 16 bits per component. The same note about the sign as
 * in {@link ByteLookupTable} applies.
 */
public class ShortLookupTable extends LookupTable {

    private final short[][] data;

    /**
     * One table per component.
     *
     * <p>The arrays **are not copied**: the table keeps the ones it is given, and only the array of
     * arrays is its own. It is what the JDK does and what allows a big table to be shared between
     * several filters without duplicating it.
     *
     * @throws IllegalArgumentException if the offset is negative
     */
    public ShortLookupTable(int offset, short[][] data) {
        super(offset, data.length);
        this.data = new short[data.length][];
        for (int i = 0; i < data.length; i++) {
            this.data[i] = data[i];
        }
    }

    /** A single table, which is applied to every component. */
    public ShortLookupTable(int offset, short[] data) {
        super(offset, 1);
        this.data = new short[1][];
        this.data[0] = data;
    }

    /** The tables, without copying. */
    public final short[][] getTable() {
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
                out[i] = this.data[0][src[i] - this.getOffset()] & 0xFFFF;
            }
        } else {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[i][src[i] - this.getOffset()] & 0xFFFF;
            }
        }
        return out;
    }

    /** The same as the other form, with the type of this table. */
    public short[] lookupPixel(short[] src, short[] dst) {
        short[] out = dst == null ? new short[src.length] : dst;
        if (this.data.length == 1) {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[0][(src[i] & 0xFFFF) - this.getOffset()];
            }
        } else {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[i][(src[i] & 0xFFFF) - this.getOffset()];
            }
        }
        return out;
    }
}
