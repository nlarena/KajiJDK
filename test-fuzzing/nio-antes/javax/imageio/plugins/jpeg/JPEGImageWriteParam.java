package javax.imageio.plugins.jpeg;

import java.util.Locale;
import javax.imageio.ImageWriteParam;

/**
 * KajiLibrary's javax.imageio.plugins.jpeg.JPEGImageWriteParam -- JPEG's own parameters when
 * writing.
 *
 * <p>It brings three things on top of {@link ImageWriteParam}: the tables to write an abbreviated
 * JPEG --see {@link JPEGImageReadParam}--, Huffman table optimization, and the quality
 * descriptions.
 *
 * <h2>The quality ranges</h2>
 *
 * <p>{@link #getCompressionQualityDescriptions} returns three names and
 * {@link #getCompressionQualityValues} <b>four</b> numbers: they are the limits of the three
 * ranges. The default quality is 0.75, exactly the limit between "medium" and "visually
 * lossless".
 *
 * <p>That 0.75 is why a JPEG saved with the factory settings looks good and does not take too much
 * space.
 *
 * <h2>{@link #setOptimizeHuffmanTables}</h2>
 *
 * <p>Off by default, and turning it on makes the file a few percent smaller while losing
 * <b>nothing</b> in quality. The cost is one more pass over the image to count frequencies and
 * build tailored tables, instead of using the annex K ones.
 *
 * <p>It is the cheapest improvement JPEG has and almost nobody uses it.
 *
 * <h2>{@link #unsetCompression} does not go back to zero</h2>
 *
 * <p>It leaves the quality at 0.75 and the type at {@code "JPEG"} -- not at null, as the base
 * class would. JPEG <b>always</b> compresses, so "no compression" is not a possible state.
 */
public class JPEGImageWriteParam extends ImageWriteParam {

    /** The quality ranges. See the class note. */
    private static final String[] QUALITY_DESCRIPTIONS = {
        "Low quality",
        "Medium quality",
        "Visually lossless",
    };

    /** Their limits; one more than the descriptions. */
    private static final float[] QUALITY_VALUES = { 0.00F, 0.30F, 0.75F, 1.00F };

    /** The quantization ones, or null. */
    private JPEGQTable[] qTables = null;

    /** The DC ones, or null. */
    private JPEGHuffmanTable[] DCHuffmanTables = null;

    /** The AC ones, or null. */
    private JPEGHuffmanTable[] ACHuffmanTables = null;

    /** Whether to build tables tailored to the image. See the class note. */
    private boolean optimizeHuffmanTables = false;

    /**
     * @param locale which locale to give the texts in, or null
     */
    public JPEGImageWriteParam(Locale locale) {
        super(locale);
        this.canWriteProgressive = true;
        this.progressiveMode = MODE_DISABLED;
        this.canWriteCompressed = true;
        this.compressionTypes = new String[] { "JPEG" };
        this.compressionType = this.compressionTypes[0];
        this.compressionQuality = 0.75F;
    }

    /**
     * Goes back to the factory quality and type.
     *
     * <p>See the class note: it does not leave the type at null, because JPEG always compresses.
     *
     * @throws IllegalStateException if the mode is not {@link #MODE_EXPLICIT}
     */
    @Override
    public void unsetCompression() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        this.compressionQuality = 0.75F;
        this.compressionType = this.compressionTypes[0];
    }

    /**
     * No: JPEG always loses.
     *
     * @throws IllegalStateException if the mode is not {@link #MODE_EXPLICIT}
     */
    @Override
    public boolean isCompressionLossless() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return false;
    }

    /**
     * The three quality ranges. See the class note.
     *
     * @throws IllegalStateException if the mode is not {@link #MODE_EXPLICIT}
     */
    @Override
    public String[] getCompressionQualityDescriptions() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        String[] copy = new String[QUALITY_DESCRIPTIONS.length];
        System.arraycopy(QUALITY_DESCRIPTIONS, 0, copy, 0, QUALITY_DESCRIPTIONS.length);
        return copy;
    }

    /**
     * Their four limits.
     *
     * @throws IllegalStateException if the mode is not {@link #MODE_EXPLICIT}
     */
    @Override
    public float[] getCompressionQualityValues() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        float[] copy = new float[QUALITY_VALUES.length];
        System.arraycopy(QUALITY_VALUES, 0, copy, 0, QUALITY_VALUES.length);
        return copy;
    }

    /** Whether tables are set. */
    public boolean areTablesSet() {
        return this.qTables != null;
    }

    /**
     * Sets the three sets; see {@link JPEGImageReadParam#setDecodeTables}.
     *
     * @throws IllegalArgumentException if any is null, if they are empty, or if the two Huffman
     *     ones do not have the same length
     */
    public void setEncodeTables(JPEGQTable[] qTables, JPEGHuffmanTable[] DCHuffmanTables,
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
    public void unsetEncodeTables() {
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

    /** Whether to build tailored Huffman tables. See the class note: worth it. */
    public void setOptimizeHuffmanTables(boolean optimize) {
        this.optimizeHuffmanTables = optimize;
    }

    /** Whether they will be tailored. */
    public boolean getOptimizeHuffmanTables() {
        return this.optimizeHuffmanTables;
    }
}
