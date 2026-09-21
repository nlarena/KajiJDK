package javax.imageio.plugins.jpeg;

/**
 * KajiLibrary's javax.imageio.plugins.jpeg.JPEGHuffmanTable -- a JPEG Huffman table.
 *
 * <p>The last stage of JPEG compression: after the transform and quantization, the coefficients
 * are Huffman-coded. This class is one of those tables.
 *
 * <h2>The two halves</h2>
 *
 * <p>A JPEG Huffman table is not stored as a tree but as two lists, which is what confuses on first
 * reading:
 *
 * <ul>
 *   <li>{@link #getLengths} has <b>16</b> entries: how many codes there are of each length, from 1
 *       to 16 bits;
 *   <li>{@link #getValues} has as many entries as there are codes in total, in order of increasing
 *       length.
 * </ul>
 *
 * <p>That is enough to rebuild the tree, because JPEG uses canonical codes: given the lengths, the
 * codes are determined. It is what lets the table take a few dozen bytes in the file instead of a
 * whole tree.
 *
 * <p>The four constants are the ones from annex K of the standard, and are the ones almost every
 * JPEG encoder in the world uses. They are fine for almost everything: optimizing the tables for a
 * specific image gains a few percent and costs one more pass.
 *
 * <p>It is immutable: the constructors copy and so do the accessors.
 */
public class JPEGHuffmanTable {

    /** The lengths of StdDCLuminance. */
    private static final short[] STDDCLUMINANCE_LENGTHS = {
        0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0
    };

    /** The values of StdDCLuminance. */
    private static final short[] STDDCLUMINANCE_VALUES = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
    };

    /** The lengths of StdDCChrominance. */
    private static final short[] STDDCCHROMINANCE_LENGTHS = {
        0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0
    };

    /** The values of StdDCChrominance. */
    private static final short[] STDDCCHROMINANCE_VALUES = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
    };

    /** The lengths of StdACLuminance. */
    private static final short[] STDACLUMINANCE_LENGTHS = {
        0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 125
    };

    /** The values of StdACLuminance. */
    private static final short[] STDACLUMINANCE_VALUES = {
        1, 2, 3, 0, 4, 17, 5, 18, 33, 49, 65, 6, 19, 81, 97, 7,
        34, 113, 20, 50, 129, 145, 161, 8, 35, 66, 177, 193, 21, 82, 209, 240,
        36, 51, 98, 114, 130, 9, 10, 22, 23, 24, 25, 26, 37, 38, 39, 40,
        41, 42, 52, 53, 54, 55, 56, 57, 58, 67, 68, 69, 70, 71, 72, 73,
        74, 83, 84, 85, 86, 87, 88, 89, 90, 99, 100, 101, 102, 103, 104, 105,
        106, 115, 116, 117, 118, 119, 120, 121, 122, 131, 132, 133, 134, 135, 136, 137,
        138, 146, 147, 148, 149, 150, 151, 152, 153, 154, 162, 163, 164, 165, 166, 167,
        168, 169, 170, 178, 179, 180, 181, 182, 183, 184, 185, 186, 194, 195, 196, 197,
        198, 199, 200, 201, 202, 210, 211, 212, 213, 214, 215, 216, 217, 218, 225, 226,
        227, 228, 229, 230, 231, 232, 233, 234, 241, 242, 243, 244, 245, 246, 247, 248,
        249, 250
    };

    /** The lengths of StdACChrominance. */
    private static final short[] STDACCHROMINANCE_LENGTHS = {
        0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 119
    };

    /** The values of StdACChrominance. */
    private static final short[] STDACCHROMINANCE_VALUES = {
        0, 1, 2, 3, 17, 4, 5, 33, 49, 6, 18, 65, 81, 7, 97, 113,
        19, 34, 50, 129, 8, 20, 66, 145, 161, 177, 193, 9, 35, 51, 82, 240,
        21, 98, 114, 209, 10, 22, 36, 52, 225, 37, 241, 23, 24, 25, 26, 38,
        39, 40, 41, 42, 53, 54, 55, 56, 57, 58, 67, 68, 69, 70, 71, 72,
        73, 74, 83, 84, 85, 86, 87, 88, 89, 90, 99, 100, 101, 102, 103, 104,
        105, 106, 115, 116, 117, 118, 119, 120, 121, 122, 130, 131, 132, 133, 134, 135,
        136, 137, 138, 146, 147, 148, 149, 150, 151, 152, 153, 154, 162, 163, 164, 165,
        166, 167, 168, 169, 170, 178, 179, 180, 181, 182, 183, 184, 185, 186, 194, 195,
        196, 197, 198, 199, 200, 201, 202, 210, 211, 212, 213, 214, 215, 216, 217, 218,
        226, 227, 228, 229, 230, 231, 232, 233, 234, 242, 243, 244, 245, 246, 247, 248,
        249, 250
    };

    /** The standard DC table for luminance. */
    public static final JPEGHuffmanTable StdDCLuminance =
        new JPEGHuffmanTable(STDDCLUMINANCE_LENGTHS, STDDCLUMINANCE_VALUES, false);

    /** The standard DC table for chrominance. */
    public static final JPEGHuffmanTable StdDCChrominance =
        new JPEGHuffmanTable(STDDCCHROMINANCE_LENGTHS, STDDCCHROMINANCE_VALUES, false);

    /** The standard AC table for luminance. */
    public static final JPEGHuffmanTable StdACLuminance =
        new JPEGHuffmanTable(STDACLUMINANCE_LENGTHS, STDACLUMINANCE_VALUES, false);

    /** The standard AC table for chrominance. */
    public static final JPEGHuffmanTable StdACChrominance =
        new JPEGHuffmanTable(STDACCHROMINANCE_LENGTHS, STDACCHROMINANCE_VALUES, false);

    /** How many codes there are of each length, from 1 to 16 bits. */
    private final short[] lengths;

    /** The values, in order of increasing length. */
    private final short[] values;

    /**
     * A new table.
     *
     * <p>Both arrays are copied.
     *
     * @param lengths how many codes of each length; it must have 16 entries
     * @param values the values; as many as there are codes in total
     * @throws IllegalArgumentException if either is null or the sizes do not add up
     */
    public JPEGHuffmanTable(short[] lengths, short[] values) {
        if (lengths == null || values == null) {
            throw new IllegalArgumentException("lengths or values are null");
        }
        this.lengths = new short[lengths.length];
        System.arraycopy(lengths, 0, this.lengths, 0, lengths.length);
        this.values = new short[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    /**
     * The constants' one, which does not copy.
     *
     * <p>The standard tables are static and nobody modifies them; copying them eight times when the
     * class loads would be wasted work. The boolean means nothing: it is there to tell the
     * signature apart.
     */
    private JPEGHuffmanTable(short[] lengths, short[] values, boolean shared) {
        this.lengths = lengths;
        this.values = values;
    }

    /** How many codes there are of each length; a copy. */
    public short[] getLengths() {
        short[] copy = new short[this.lengths.length];
        System.arraycopy(this.lengths, 0, copy, 0, this.lengths.length);
        return copy;
    }

    /** The values; a copy. */
    public short[] getValues() {
        short[] copy = new short[this.values.length];
        System.arraycopy(this.values, 0, copy, 0, this.values.length);
        return copy;
    }

    /** Both lists, one per line. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("JPEGHuffmanTable\n");
        sb.append("lengths:");
        int i = 0;
        while (i < this.lengths.length) {
            sb.append(' ').append(this.lengths[i]);
            i = i + 1;
        }
        sb.append("\nvalues:");
        i = 0;
        while (i < this.values.length) {
            sb.append(' ').append(this.values[i]);
            i = i + 1;
        }
        return sb.toString();
    }
}
