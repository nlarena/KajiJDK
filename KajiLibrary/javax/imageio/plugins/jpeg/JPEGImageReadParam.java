package javax.imageio.plugins.jpeg;

import javax.imageio.ImageReadParam;

/**
 * KajiLibrary's javax.imageio.plugins.jpeg.JPEGImageReadParam -- the tables to decode a JPEG that
 * lacks them.
 *
 * <p>It exists for one concrete and uncommon case: <b>abbreviated</b> JPEGs. The standard allows a
 * stream that carries the compressed data but <b>not</b> the tables it was compressed with --
 * because they travel separately, in a tables-only stream, or because sender and receiver agreed
 * on them beforehand.
 *
 * <p>A JPEG like that cannot be decoded on its own. {@link #setDecodeTables} is how the missing
 * tables are passed in.
 *
 * <h2>The three arrays go together</h2>
 *
 * <p>Quantization, DC Huffman and AC Huffman. The three are set at once and removed at once: there
 * is no way to set just one, because an incomplete set is no use for decoding anything.
 *
 * <p>Passing null in any of the three throws {@link IllegalArgumentException}; to remove them there
 * is {@link #unsetDecodeTables}.
 *
 * <p>For a normal JPEG --which is ninety-nine percent of them-- this class is not needed: the
 * tables come in the file.
 */
public class JPEGImageReadParam extends ImageReadParam {

    /** The quantization ones, or null. */
    private JPEGQTable[] qTables = null;

    /** The DC Huffman ones, or null. */
    private JPEGHuffmanTable[] DCHuffmanTables = null;

    /** The AC ones, or null. */
    private JPEGHuffmanTable[] ACHuffmanTables = null;

    /** With no tables set. */
    public JPEGImageReadParam() {
    }

    /** Whether tables are set. */
    public boolean areTablesSet() {
        return this.qTables != null;
    }

    /**
     * Sets the three sets. See the class note: they go together.
     *
     * <p>The arrays are copied.
     *
     * @throws IllegalArgumentException if any is null, if they are empty, or if the two Huffman
     *     ones do not have the same length
     */
    public void setDecodeTables(JPEGQTable[] qTables, JPEGHuffmanTable[] DCHuffmanTables,
                                JPEGHuffmanTable[] ACHuffmanTables) {
        if (qTables == null || DCHuffmanTables == null || ACHuffmanTables == null
            || qTables.length == 0 || DCHuffmanTables.length == 0 || ACHuffmanTables.length == 0
            || DCHuffmanTables.length != ACHuffmanTables.length) {
            throw new IllegalArgumentException("Invalid JPEG table arrays");
        }
        this.qTables = new JPEGQTable[qTables.length];
        System.arraycopy(qTables, 0, this.qTables, 0, qTables.length);
        this.DCHuffmanTables = new JPEGHuffmanTable[DCHuffmanTables.length];
        System.arraycopy(DCHuffmanTables, 0, this.DCHuffmanTables, 0, DCHuffmanTables.length);
        this.ACHuffmanTables = new JPEGHuffmanTable[ACHuffmanTables.length];
        System.arraycopy(ACHuffmanTables, 0, this.ACHuffmanTables, 0, ACHuffmanTables.length);
    }

    /** Removes them. */
    public void unsetDecodeTables() {
        this.qTables = null;
        this.DCHuffmanTables = null;
        this.ACHuffmanTables = null;
    }

    /** The quantization ones, or null; a copy. */
    public JPEGQTable[] getQTables() {
        if (this.qTables == null) {
            return null;
        }
        JPEGQTable[] copy = new JPEGQTable[this.qTables.length];
        System.arraycopy(this.qTables, 0, copy, 0, this.qTables.length);
        return copy;
    }

    /** The DC ones, or null; a copy. */
    public JPEGHuffmanTable[] getDCHuffmanTables() {
        if (this.DCHuffmanTables == null) {
            return null;
        }
        JPEGHuffmanTable[] copy = new JPEGHuffmanTable[this.DCHuffmanTables.length];
        System.arraycopy(this.DCHuffmanTables, 0, copy, 0, this.DCHuffmanTables.length);
        return copy;
    }

    /** The AC ones, or null; a copy. */
    public JPEGHuffmanTable[] getACHuffmanTables() {
        if (this.ACHuffmanTables == null) {
            return null;
        }
        JPEGHuffmanTable[] copy = new JPEGHuffmanTable[this.ACHuffmanTables.length];
        System.arraycopy(this.ACHuffmanTables, 0, copy, 0, this.ACHuffmanTables.length);
        return copy;
    }
}
