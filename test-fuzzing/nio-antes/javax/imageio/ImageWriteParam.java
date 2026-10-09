package javax.imageio;

import java.awt.Dimension;
import java.util.Locale;

/**
 * KajiLibrary's javax.imageio.ImageWriteParam -- how to write an image.
 *
 * <p>It adds to {@link IIOParam} what only makes sense when writing: tiling, progressiveness and
 * compression. All three work the same way, and that pattern is the whole class.
 *
 * <h2>The four modes, and why there are four</h2>
 *
 * <p>Each of the three features is controlled with a <b>mode</b>:
 *
 * <ul>
 *   <li>{@link #MODE_DISABLED}: do not do it. No tiling, no progressiveness, no compression;
 *   <li>{@link #MODE_DEFAULT}: do it the way the writer prefers. You cannot query with which
 *       parameters -- asking for the tile width in this mode throws {@link IllegalStateException};
 *   <li>{@link #MODE_EXPLICIT}: do it with the parameters given. It is the only one where the
 *       concrete {@code setXxx} and {@code getXxx} are valid;
 *   <li>{@link #MODE_COPY_FROM_METADATA}: take them from the image's metadata. It is the
 *       <b>default</b> mode, and the right one when rewriting something that was read.
 * </ul>
 *
 * <p>That the default mode is the last one is surprising. It makes sense: the most common operation
 * is reading and writing back, and there what you want is to keep what the original file said.
 *
 * <h2>Two different exceptions</h2>
 *
 * <p>It is what confuses most in this class:
 *
 * <ul>
 *   <li>{@link UnsupportedOperationException} means "<b>this writer</b> cannot do that". You ask
 *       beforehand with {@link #canWriteTiles} and friends;
 *   <li>{@link IllegalStateException} means "it can, but the mode is not
 *       {@link #MODE_EXPLICIT}", or "the parameters have not been set yet".
 * </ul>
 *
 * <p>The first is a problem of choosing the writer; the second, of the order of the calls.
 *
 * <h2>Quality goes the opposite way to what it seems</h2>
 *
 * <p>{@link #setCompressionQuality} takes a value from 0 to 1 where <b>1 is the best quality</b>
 * --and the largest file. It is the opposite of a "compression level", which in other APIs goes
 * from less to more. The default is 1.
 */
public class ImageWriteParam extends IIOParam {

    /** Do not do it. */
    public static final int MODE_DISABLED = 0;

    /** Do it the way the writer prefers. See the class note. */
    public static final int MODE_DEFAULT = 1;

    /** Do it with the given parameters. */
    public static final int MODE_EXPLICIT = 2;

    /** Take them from the metadata. It is the default. */
    public static final int MODE_COPY_FROM_METADATA = 3;

    /** Whether this writer can tile. */
    protected boolean canWriteTiles = false;

    /** Which mode tiling is in. */
    protected int tilingMode = MODE_COPY_FROM_METADATA;

    /** The tile sizes it prefers, or null. */
    protected Dimension[] preferredTileSizes = null;

    /** Whether the tiling parameters have been set. */
    protected boolean tilingSet = false;

    /** Tile width. */
    protected int tileWidth = 0;

    /** Tile height. */
    protected int tileHeight = 0;

    /** Whether it can offset the tile grid. */
    protected boolean canOffsetTiles = false;

    /** Grid offset in X. */
    protected int tileGridXOffset = 0;

    /** Same in Y. */
    protected int tileGridYOffset = 0;

    /** Whether it can write progressively. */
    protected boolean canWriteProgressive = false;

    /** Which mode progressiveness is in. */
    protected int progressiveMode = MODE_COPY_FROM_METADATA;

    /** Whether it can compress. */
    protected boolean canWriteCompressed = false;

    /** Which mode compression is in. */
    protected int compressionMode = MODE_COPY_FROM_METADATA;

    /** The compression types it supports, or null. */
    protected String[] compressionTypes = null;

    /** The chosen one, or null. */
    protected String compressionType = null;

    /** The quality, from 0 to 1. See the class note. */
    protected float compressionQuality = 1.0F;

    /** Which locale to give the texts in, or null. */
    protected Locale locale = null;

    /** For the subclasses, which set the capabilities. */
    protected ImageWriteParam() {
    }

    /** @param locale which locale to give the texts in, or null */
    public ImageWriteParam(Locale locale) {
        this.locale = locale;
    }

    /** Which locale, or null. */
    public Locale getLocale() {
        return this.locale;
    }

    /** Whether this writer can tile. */
    public boolean canWriteTiles() {
        return this.canWriteTiles;
    }

    /** Whether it can offset the grid. */
    public boolean canOffsetTiles() {
        return this.canOffsetTiles;
    }

    /**
     * Sets the tiling mode.
     *
     * @throws UnsupportedOperationException if it cannot tile
     * @throws IllegalArgumentException if the mode is not one of the four (an earlier note added a
     *     clause about offsets; this method takes none and does not check any)
     */
    public void setTilingMode(int mode) {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
        if (mode < MODE_DISABLED || mode > MODE_COPY_FROM_METADATA) {
            throw new IllegalArgumentException("Illegal value for mode!");
        }
        this.tilingMode = mode;
        if (mode == MODE_EXPLICIT) {
            unsetTiling();
        }
    }

    /**
     * Which mode it is in.
     *
     * @throws UnsupportedOperationException if it cannot tile
     */
    public int getTilingMode() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported");
        }
        return this.tilingMode;
    }

    /**
     * The tile sizes it prefers, in min/max pairs; null if it has no opinion.
     *
     * @throws UnsupportedOperationException if it cannot tile
     */
    public Dimension[] getPreferredTileSizes() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported");
        }
        if (this.preferredTileSizes == null) {
            return null;
        }
        Dimension[] copy = new Dimension[this.preferredTileSizes.length];
        int i = 0;
        while (i < this.preferredTileSizes.length) {
            copy[i] = (Dimension) this.preferredTileSizes[i].clone();
            i = i + 1;
        }
        return copy;
    }

    /**
     * Sets the size and the offset of the tiles.
     *
     * @throws UnsupportedOperationException if it cannot tile, or if an offset is asked for and it
     *     cannot offset
     * @throws IllegalStateException if the mode is not {@link #MODE_EXPLICIT}
     * @throws IllegalArgumentException if the width or height are not positive, or if they are not
     *     among the preferred sizes
     */
    public void setTiling(int tileWidth, int tileHeight, int tileGridXOffset,
                          int tileGridYOffset) {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException("tile dimensions are non-positive!");
        }
        boolean canOffset = canOffsetTiles();
        if (!canOffset && (tileGridXOffset != 0 || tileGridYOffset != 0)) {
            throw new UnsupportedOperationException("Can't offset tiles!");
        }
        if (this.preferredTileSizes != null) {
            // The preferred sizes come in pairs: min and max of each acceptable range.
            boolean ok = false;
            int i = 0;
            while (i < this.preferredTileSizes.length && !ok) {
                Dimension min = this.preferredTileSizes[i];
                Dimension max = this.preferredTileSizes[i + 1];
                if (tileWidth >= min.width && tileWidth <= max.width
                    && tileHeight >= min.height && tileHeight <= max.height) {
                    ok = true;
                }
                i = i + 2;
            }
            if (!ok) {
                throw new IllegalArgumentException("Illegal tile size!");
            }
        }
        this.tilingSet = true;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tileGridXOffset = tileGridXOffset;
        this.tileGridYOffset = tileGridYOffset;
    }

    /**
     * Forgets the tiling parameters.
     *
     * @throws UnsupportedOperationException if it cannot tile
     * @throws IllegalStateException if the mode is not {@link #MODE_EXPLICIT}
     */
    public void unsetTiling() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        this.tilingSet = false;
        this.tileWidth = 0;
        this.tileHeight = 0;
        this.tileGridXOffset = 0;
        this.tileGridYOffset = 0;
    }

    /**
     * The tile width.
     *
     * @throws UnsupportedOperationException if it cannot tile
     * @throws IllegalStateException if the mode is not explicit or the parameters were not set
     */
    public int getTileWidth() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        if (!this.tilingSet) {
            throw new IllegalStateException("Tiling parameters not set!");
        }
        return this.tileWidth;
    }

    /** The height. Same conditions as {@link #getTileWidth}. */
    public int getTileHeight() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        if (!this.tilingSet) {
            throw new IllegalStateException("Tiling parameters not set!");
        }
        return this.tileHeight;
    }

    /**
     * The grid offset in X.
     *
     * @throws UnsupportedOperationException if it cannot offset tiles
     * @throws IllegalStateException if the mode is not explicit or the parameters were not set
     */
    public int getTileGridXOffset() {
        if (!canOffsetTiles()) {
            throw new UnsupportedOperationException("Tile offsets not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        if (!this.tilingSet) {
            throw new IllegalStateException("Tiling parameters not set!");
        }
        return this.tileGridXOffset;
    }

    /** Same in Y. */
    public int getTileGridYOffset() {
        if (!canOffsetTiles()) {
            throw new UnsupportedOperationException("Tile offsets not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        if (!this.tilingSet) {
            throw new IllegalStateException("Tiling parameters not set!");
        }
        return this.tileGridYOffset;
    }

    /** Whether this writer can write progressively. */
    public boolean canWriteProgressive() {
        return this.canWriteProgressive;
    }

    /**
     * Sets the progressive mode.
     *
     * <p>{@link #MODE_EXPLICIT} is <b>not</b> accepted here: there are no parameters to give, a
     * file is progressive or it is not.
     *
     * @throws UnsupportedOperationException if it cannot
     * @throws IllegalArgumentException if the mode is {@link #MODE_EXPLICIT} or not one of the four
     */
    public void setProgressiveMode(int mode) {
        if (!canWriteProgressive()) {
            throw new UnsupportedOperationException("Progressive output not supported");
        }
        if (mode < MODE_DISABLED || mode > MODE_COPY_FROM_METADATA) {
            throw new IllegalArgumentException("Illegal value for mode!");
        }
        if (mode == MODE_EXPLICIT) {
            throw new IllegalArgumentException(
                "MODE_EXPLICIT not supported for progressive output");
        }
        this.progressiveMode = mode;
    }

    /**
     * Which mode it is in.
     *
     * @throws UnsupportedOperationException if it cannot
     */
    public int getProgressiveMode() {
        if (!canWriteProgressive()) {
            throw new UnsupportedOperationException("Progressive output not supported");
        }
        return this.progressiveMode;
    }

    /** Whether this writer can compress. */
    public boolean canWriteCompressed() {
        return this.canWriteCompressed;
    }

    /**
     * Sets the compression mode.
     *
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalArgumentException if the mode is not one of the four
     */
    public void setCompressionMode(int mode) {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (mode < MODE_DISABLED || mode > MODE_COPY_FROM_METADATA) {
            throw new IllegalArgumentException("Illegal value for mode!");
        }
        this.compressionMode = mode;
        if (mode == MODE_EXPLICIT) {
            unsetCompression();
        }
    }

    /**
     * Which mode it is in.
     *
     * @throws UnsupportedOperationException if it cannot compress
     */
    public int getCompressionMode() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        return this.compressionMode;
    }

    /**
     * The compression types it supports, or null if there are not several.
     *
     * @throws UnsupportedOperationException if it cannot compress
     */
    public String[] getCompressionTypes() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (this.compressionTypes == null) {
            return null;
        }
        String[] copy = new String[this.compressionTypes.length];
        System.arraycopy(this.compressionTypes, 0, copy, 0, this.compressionTypes.length);
        return copy;
    }

    /**
     * Chooses the compression type.
     *
     * @param compressionType one of {@link #getCompressionTypes}; null unsets it
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalStateException if the mode is not {@link #MODE_EXPLICIT}
     * @throws IllegalArgumentException if it is not one of the supported ones
     */
    public void setCompressionType(String compressionType) {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        String[] legalTypes = getCompressionTypes();
        if (compressionType == null) {
            this.compressionType = null;
            return;
        }
        if (legalTypes == null) {
            throw new UnsupportedOperationException("No settable compression types");
        }
        boolean found = false;
        int i = 0;
        while (i < legalTypes.length) {
            if (compressionType.equals(legalTypes[i])) {
                found = true;
            }
            i = i + 1;
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown compression type!");
        }
        this.compressionType = compressionType;
    }

    /**
     * The chosen one, or null.
     *
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalStateException if the mode is not {@link #MODE_EXPLICIT}
     */
    public String getCompressionType() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return this.compressionType;
    }

    /**
     * Forgets the type and the quality.
     *
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalStateException if the mode is not {@link #MODE_EXPLICIT}
     */
    public void unsetCompression() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        this.compressionType = null;
        this.compressionQuality = 1.0F;
    }

    /**
     * The chosen type's name, in the requested locale.
     *
     * <p>This implementation returns the name as is: translating it takes a text catalogue only the
     * concrete writer has.
     *
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalStateException if the mode is not explicit or no type is chosen
     */
    public String getLocalizedCompressionTypeName() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        String type = getCompressionType();
        if (type == null) {
            throw new IllegalStateException("No compression type set!");
        }
        return type;
    }

    /**
     * Whether the chosen compression keeps everything.
     *
     * <p>Returns true by default: an implementation that can compress lossily <b>has</b> to
     * redefine it. (Saying "lossless" when there is loss would mislead; the other way round would
     * only make someone recompress needlessly -- which is why a lossy writer must not inherit
     * this.)
     *
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalStateException if the mode is not explicit
     */
    public boolean isCompressionLossless() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return true;
    }

    /**
     * Sets the quality, from 0 to 1. See the class note: 1 is the best.
     *
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalStateException if the mode is not explicit
     * @throws IllegalArgumentException if it is out of range
     */
    public void setCompressionQuality(float quality) {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        if (quality < 0 || quality > 1.0F) {
            throw new IllegalArgumentException("Quality out-of-bounds!");
        }
        this.compressionQuality = quality;
    }

    /**
     * The quality.
     *
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalStateException if the mode is not explicit
     */
    public float getCompressionQuality() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return this.compressionQuality;
    }

    /**
     * How many bits per pixel that quality would give, or -1 if unknown.
     *
     * <p>This implementation always returns -1: estimating it depends on the concrete encoder. -1
     * is the value the documentation defines for "I do not know".
     *
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalStateException if the mode is not explicit
     * @throws IllegalArgumentException if the quality is out of range
     */
    public float getBitRate(float quality) {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        if (quality < 0.0F || quality > 1.0F) {
            throw new IllegalArgumentException("Quality out-of-bounds!");
        }
        return -1.0F;
    }

    /**
     * How to describe the quality ranges, or null if there are no descriptions.
     *
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalStateException if the mode is not explicit
     */
    public String[] getCompressionQualityDescriptions() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return null;
    }

    /**
     * The limits of those ranges, or null.
     *
     * <p>If there are {@code n} descriptions, there are {@code n + 1} limits: they pair up with the
     * ranges they separate.
     *
     * @throws UnsupportedOperationException if it cannot compress
     * @throws IllegalStateException if the mode is not explicit
     */
    public float[] getCompressionQualityValues() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return null;
    }
}
