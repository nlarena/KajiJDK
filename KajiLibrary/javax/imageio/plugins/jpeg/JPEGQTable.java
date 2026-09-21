package javax.imageio.plugins.jpeg;

/**
 * KajiLibrary's javax.imageio.plugins.jpeg.JPEGQTable -- a JPEG quantization table.
 *
 * <p>This is where JPEG <b>loses information</b>, and it is the only thing to understand about this
 * class. Each of the 64 coefficients of a block is divided by its table entry and rounded; what is
 * lost in that rounding does not come back.
 *
 * <p>That is why the numbers grow towards the end: the first entries are the low frequencies --the
 * large shapes, which the eye sees-- and the last ones the high frequencies --the fine detail,
 * which the eye hardly sees. Dividing the high ones by 99 and the low ones by 16 is exactly the bet
 * JPEG makes.
 *
 * <p>The 64 values are in <b>natural order</b>, by rows: entry {@code i} is row {@code i / 8},
 * column {@code i % 8}. In the file they go in zigzag; the conversion is not this class's
 * business.
 *
 * <h2>{@link #getScaledInstance}</h2>
 *
 * <p>It is how a quality control is implemented: multiplying the whole table by a factor. Less than
 * one gives more quality and more size; more than one, the other way round.
 *
 * <p>The boolean decides the ceiling: {@code true} clips to 255 --which 8-bit JPEG requires-- and
 * {@code false} to 32767. The floor is always 1, because dividing by zero is not an option.
 *
 * <p>The four constants are the ones from annex K of the standard. The {@code Div2} ones are the
 * same halved, which is roughly quality 75 against quality 50.
 *
 * <p>It is immutable.
 */
public class JPEGQTable {

    /** The 64 values of K1Luminance, in natural order. */
    private static final int[] K1LUMINANCE_TABLE = {
        16, 11, 10, 16, 24, 40, 51, 61,
        12, 12, 14, 19, 26, 58, 60, 55,
        14, 13, 16, 24, 40, 57, 69, 56,
        14, 17, 22, 29, 51, 87, 80, 62,
        18, 22, 37, 56, 68, 109, 103, 77,
        24, 35, 55, 64, 81, 104, 113, 92,
        49, 64, 78, 87, 103, 121, 120, 101,
        72, 92, 95, 98, 112, 100, 103, 99
    };

    /** The 64 values of K1Div2Luminance, in natural order. */
    private static final int[] K1DIV2LUMINANCE_TABLE = {
        8, 6, 5, 8, 12, 20, 26, 31,
        6, 6, 7, 10, 13, 29, 30, 28,
        7, 7, 8, 12, 20, 29, 35, 28,
        7, 9, 11, 15, 26, 44, 40, 31,
        9, 11, 19, 28, 34, 55, 52, 39,
        12, 18, 28, 32, 41, 52, 57, 46,
        25, 32, 39, 44, 52, 61, 60, 51,
        36, 46, 48, 49, 56, 50, 52, 50
    };

    /** The 64 values of K2Chrominance, in natural order. */
    private static final int[] K2CHROMINANCE_TABLE = {
        17, 18, 24, 47, 99, 99, 99, 99,
        18, 21, 26, 66, 99, 99, 99, 99,
        24, 26, 56, 99, 99, 99, 99, 99,
        47, 66, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99
    };

    /** The 64 values of K2Div2Chrominance, in natural order. */
    private static final int[] K2DIV2CHROMINANCE_TABLE = {
        9, 9, 12, 24, 50, 50, 50, 50,
        9, 11, 13, 33, 50, 50, 50, 50,
        12, 13, 28, 50, 50, 50, 50, 50,
        24, 33, 50, 50, 50, 50, 50, 50,
        50, 50, 50, 50, 50, 50, 50, 50,
        50, 50, 50, 50, 50, 50, 50, 50,
        50, 50, 50, 50, 50, 50, 50, 50,
        50, 50, 50, 50, 50, 50, 50, 50
    };

    /** The standard's table K.1, for luminance. */
    public static final JPEGQTable K1Luminance = new JPEGQTable(K1LUMINANCE_TABLE, false);

    /** K.1 halved: higher quality and larger files. */
    public static final JPEGQTable K1Div2Luminance = new JPEGQTable(K1DIV2LUMINANCE_TABLE, false);

    /** The standard's table K.2, for chrominance. */
    public static final JPEGQTable K2Chrominance = new JPEGQTable(K2CHROMINANCE_TABLE, false);

    /** K.2 halved. */
    public static final JPEGQTable K2Div2Chrominance = new JPEGQTable(K2DIV2CHROMINANCE_TABLE, false);

    /** The 64 values, in natural order. */
    private final int[] qTable;

    /**
     * A new table; the array is copied.
     *
     * @param table 64 values in natural order
     * @throws IllegalArgumentException if it is null or does not have 64 entries
     */
    public JPEGQTable(int[] table) {
        if (table == null) {
            throw new IllegalArgumentException("table must not be null.");
        }
        if (table.length != 64) {
            throw new IllegalArgumentException("table.length != 64");
        }
        this.qTable = new int[64];
        System.arraycopy(table, 0, this.qTable, 0, 64);
    }

    /** The constants' one, which does not copy. See {@link JPEGHuffmanTable}. */
    private JPEGQTable(int[] table, boolean shared) {
        this.qTable = table;
    }

    /** The 64 values; a copy. */
    public int[] getTable() {
        int[] copy = new int[64];
        System.arraycopy(this.qTable, 0, copy, 0, 64);
        return copy;
    }

    /**
     * A table with all the values multiplied by that factor.
     *
     * <p>See the class note: the floor is 1 and the ceiling depends on the boolean.
     *
     * @param scaleFactor what to multiply by
     * @param forceBaseline whether to clip to 255 instead of 32767
     */
    public JPEGQTable getScaledInstance(float scaleFactor, boolean forceBaseline) {
        int max;
        if (forceBaseline) {
            max = 255;
        } else {
            max = 32767;
        }
        int[] scaled = new int[64];
        int i = 0;
        while (i < 64) {
            int value = Math.round(this.qTable[i] * scaleFactor);
            if (value < 1) {
                value = 1;
            }
            if (value > max) {
                value = max;
            }
            scaled[i] = value;
            i = i + 1;
        }
        return new JPEGQTable(scaled, false);
    }

    /** The 64 values in eight rows of eight, with a tab in front. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("JPEGQTable:\n");
        int row = 0;
        while (row < 8) {
            sb.append('\t');
            int col = 0;
            while (col < 8) {
                sb.append(this.qTable[row * 8 + col]);
                if (col < 7) {
                    sb.append(' ');
                }
                col = col + 1;
            }
            sb.append('\n');
            row = row + 1;
        }
        return sb.toString();
    }
}
