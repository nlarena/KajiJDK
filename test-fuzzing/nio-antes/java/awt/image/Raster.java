package java.awt.image;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Pixels with an **address**: a {@link SampleModel} plus a {@link DataBuffer} plus a rectangle.
 *
 * <p>The sample model knows how a pixel is stored and the buffer has the numbers, but neither of
 * them knows *where* the image is. That is what the raster adds: its coordinates start at
 * `(minX, minY)`, which does not have to be the origin.
 *
 * <p>From there comes the one non-obvious concept of the class, `sampleModelTranslate`. The
 * coordinates of the raster and those of the sample model are two different systems, and the
 * translation is a subtraction:
 *
 * <pre>model coordinate = raster coordinate - sampleModelTranslate</pre>
 *
 * <p>It serves so that a **child** —a crop— can share the parent's data instead of copying it. The
 * child is declared in whatever coordinates it likes and its translation absorbs the difference;
 * both rasters read the same buffer. A crop, then, costs no memory: it is a window.
 *
 * <p>This class is **read-only**. The methods that write are in {@link WritableRaster}, which
 * inherits from it. The separation is real and not decorative: `getParent()` of a read-only raster
 * can return a raster that can be written to, and not the other way round.
 *
 * <p>The constructors are protected: a raster is asked for through the static `create*` methods,
 * which choose the sample model suited to each layout.
 */
public class Raster {

    /** How the pixels are laid out. */
    protected SampleModel sampleModel;

    /** Where the numbers are. */
    protected DataBuffer dataBuffer;

    /** X coordinate of the top-left corner. */
    protected int minX;

    /** Y coordinate of the top-left corner. */
    protected int minY;

    /** Width, in pixels. */
    protected int width;

    /** Height, in pixels. */
    protected int height;

    /** What has to be subtracted from a raster X to get the model's. */
    protected int sampleModelTranslateX;

    /** What has to be subtracted from a raster Y to get the model's. */
    protected int sampleModelTranslateY;

    /** How many bands each pixel has. */
    protected int numBands;

    /** How many elements of the buffer a pixel takes. */
    protected int numDataElements;

    /** The raster this one is a crop of, or `null`. */
    protected Raster parent;

    /**
     * A raster with a new buffer, of the size of the model, placed at `origin`.
     *
     * @throws RasterFormatException if the resulting size is empty
     */
    protected Raster(SampleModel sampleModel, Point origin) {
        this(sampleModel, sampleModel.createDataBuffer(),
                new Rectangle(origin.x, origin.y, sampleModel.getWidth(), sampleModel.getHeight()),
                origin, null);
    }

    /**
     * A raster over the given buffer, of the size of the model, placed at `origin`.
     *
     * @throws RasterFormatException if the resulting size is empty
     */
    protected Raster(SampleModel sampleModel, DataBuffer dataBuffer, Point origin) {
        this(sampleModel, dataBuffer,
                new Rectangle(origin.x, origin.y, sampleModel.getWidth(), sampleModel.getHeight()),
                origin, null);
    }

    /**
     * The general constructor: region, translation and parent given separately.
     *
     * @throws NullPointerException if any of the first four is missing
     * @throws RasterFormatException if the region is empty or if its coordinates go past `int`
     */
    protected Raster(SampleModel sampleModel, DataBuffer dataBuffer, Rectangle aRegion,
            Point sampleModelTranslate, Raster parent) {
        if (sampleModel == null || dataBuffer == null || aRegion == null
                || sampleModelTranslate == null) {
            throw new NullPointerException("SampleModel, dataBuffer, aRegion and "
                    + "sampleModelTranslate cannot be null");
        }
        this.sampleModel = sampleModel;
        this.dataBuffer = dataBuffer;
        this.minX = aRegion.x;
        this.minY = aRegion.y;
        this.width = aRegion.width;
        this.height = aRegion.height;
        if (this.width <= 0 || this.height <= 0) {
            throw new RasterFormatException("negative or zero "
                    + (this.width <= 0 ? "width" : "height"));
        }
        // The sum is checked because a rectangle that goes past int would give a limit lower than
        // its own origin, and every edge check further down would pass the wrong way round.
        if (this.minX + this.width < this.minX) {
            throw new RasterFormatException("overflow condition for X coordinates of Raster");
        }
        if (this.minY + this.height < this.minY) {
            throw new RasterFormatException("overflow condition for Y coordinates of Raster");
        }
        this.sampleModelTranslateX = sampleModelTranslate.x;
        this.sampleModelTranslateY = sampleModelTranslate.y;
        this.numBands = sampleModel.getNumBands();
        this.numDataElements = sampleModel.getNumDataElements();
        this.parent = parent;
    }

    // ---- factories ----------------------------------------------------------------------------

    /**
     * An interleaved raster with the bands in order and no padding.
     *
     * @throws IllegalArgumentException if the type is neither `byte` nor `ushort`
     */
    public static WritableRaster createInterleavedRaster(int dataType, int w, int h, int bands,
            Point location) {
        int[] bandOffsets = new int[bands];
        for (int i = 0; i < bands; i++) {
            bandOffsets[i] = i;
        }
        return createInterleavedRaster(dataType, w, h, w * bands, bands, bandOffsets, location);
    }

    /**
     * An interleaved raster with the given layout, over a new buffer.
     *
     * <p>Only `byte` and `ushort`: they are the types an interleaved image makes sense in, and it
     * is the same restriction as the JDK's.
     *
     * @throws IllegalArgumentException if the type is neither `byte` nor `ushort`, or if the size
     *     is empty
     */
    public static WritableRaster createInterleavedRaster(int dataType, int w, int h,
            int scanlineStride, int pixelStride, int[] bandOffsets, Point location) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") must be > 0");
        }
        int maxBandOff = bandOffsets[0];
        for (int i = 1; i < bandOffsets.length; i++) {
            if (bandOffsets[i] > maxBandOff) {
                maxBandOff = bandOffsets[i];
            }
        }
        // The last element that is going to be touched is the furthest band of the last pixel of
        // the last row. The "+1" is because the former is an index and this a size.
        int size = maxBandOff + scanlineStride * (h - 1) + pixelStride * (w - 1) + 1;
        DataBuffer d;
        if (dataType == DataBuffer.TYPE_BYTE) {
            d = new DataBufferByte(size);
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            d = new DataBufferUShort(size);
        } else {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        return createInterleavedRaster(d, w, h, scanlineStride, pixelStride, bandOffsets, location);
    }

    /**
     * An interleaved raster over the given buffer.
     *
     * @throws NullPointerException if the buffer is `null`
     * @throws RasterFormatException if the buffer has more than one bank
     * @throws IllegalArgumentException if the type is neither `byte` nor `ushort`
     */
    public static WritableRaster createInterleavedRaster(DataBuffer dataBuffer, int w, int h,
            int scanlineStride, int pixelStride, int[] bandOffsets, Point location) {
        if (dataBuffer == null) {
            throw new NullPointerException("DataBuffer cannot be null");
        }
        Point where = location;
        if (where == null) {
            where = new Point(0, 0);
        }
        int dataType = dataBuffer.getDataType();
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        if (dataBuffer.getNumBanks() != 1) {
            throw new RasterFormatException(
                    "DataBuffer for interleaved Rasters must only have 1 bank.");
        }
        PixelInterleavedSampleModel csm = new PixelInterleavedSampleModel(dataType, w, h,
                pixelStride, scanlineStride, bandOffsets);
        return new WritableRaster(csm, dataBuffer, where);
    }

    /**
     * A raster by planes with one band per bank and no padding.
     *
     * @throws ArrayIndexOutOfBoundsException if `bands` is not positive
     */
    public static WritableRaster createBandedRaster(int dataType, int w, int h, int bands,
            Point location) {
        if (bands < 1) {
            throw new ArrayIndexOutOfBoundsException("Number of bands (" + bands
                    + ") must be greater than 0");
        }
        int[] bankIndices = new int[bands];
        int[] bandOffsets = new int[bands];
        for (int i = 0; i < bands; i++) {
            bankIndices[i] = i;
            bandOffsets[i] = 0;
        }
        return createBandedRaster(dataType, w, h, w, bankIndices, bandOffsets, location);
    }

    /**
     * A raster by planes with the given layout, over a new buffer.
     *
     * @throws ArrayIndexOutOfBoundsException if either of the two arrays is missing
     * @throws IllegalArgumentException if the arrays are not the same length, if the size is empty
     *     or if the type is neither `byte`, `ushort` nor `int`
     */
    public static WritableRaster createBandedRaster(int dataType, int w, int h, int scanlineStride,
            int[] bankIndices, int[] bandOffsets, Point location) {
        if (bankIndices == null) {
            throw new ArrayIndexOutOfBoundsException("Bank indices array is null");
        }
        if (bandOffsets == null) {
            throw new ArrayIndexOutOfBoundsException("Band offsets array is null");
        }
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") must be > 0");
        }
        if (bankIndices.length != bandOffsets.length) {
            throw new IllegalArgumentException("bankIndices.length != bandOffsets.length");
        }
        if (bandOffsets.length < 1) {
            throw new IllegalArgumentException("Must have at least one band.");
        }
        int maxBank = bankIndices[0];
        int maxBandOff = bandOffsets[0];
        for (int i = 1; i < bandOffsets.length; i++) {
            if (bankIndices[i] > maxBank) {
                maxBank = bankIndices[i];
            }
            if (bandOffsets[i] > maxBandOff) {
                maxBandOff = bandOffsets[i];
            }
        }
        int banks = maxBank + 1;
        int size = maxBandOff + scanlineStride * (h - 1) + w;
        DataBuffer d;
        if (dataType == DataBuffer.TYPE_BYTE) {
            d = new DataBufferByte(size, banks);
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            d = new DataBufferUShort(size, banks);
        } else if (dataType == DataBuffer.TYPE_INT) {
            d = new DataBufferInt(size, banks);
        } else {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        return createBandedRaster(d, w, h, scanlineStride, bankIndices, bandOffsets, location);
    }

    /**
     * A raster by planes over the given buffer.
     *
     * @throws NullPointerException if the buffer is `null`
     * @throws IllegalArgumentException if the type is neither `byte`, `ushort` nor `int`
     */
    public static WritableRaster createBandedRaster(DataBuffer dataBuffer, int w, int h,
            int scanlineStride, int[] bankIndices, int[] bandOffsets, Point location) {
        if (dataBuffer == null) {
            throw new NullPointerException("DataBuffer cannot be null");
        }
        Point where = location;
        if (where == null) {
            where = new Point(0, 0);
        }
        int dataType = dataBuffer.getDataType();
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        BandedSampleModel bsm = new BandedSampleModel(dataType, w, h, scanlineStride, bankIndices,
                bandOffsets);
        return new WritableRaster(bsm, dataBuffer, where);
    }

    /**
     * A packed raster with the bands given by their masks, over a new buffer.
     *
     * @throws IllegalArgumentException if the type is neither `byte`, `ushort` nor `int`
     */
    public static WritableRaster createPackedRaster(int dataType, int w, int h, int[] bandMasks,
            Point location) {
        DataBuffer d;
        if (dataType == DataBuffer.TYPE_BYTE) {
            d = new DataBufferByte(w * h);
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            d = new DataBufferUShort(w * h);
        } else if (dataType == DataBuffer.TYPE_INT) {
            d = new DataBufferInt(w * h);
        } else {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        return createPackedRaster(d, w, h, w, bandMasks, location);
    }

    /**
     * A packed raster with `bands` bands of `bitsPerBand` bits each.
     *
     * <p>With more than one band they are bit fields inside one pixel, and contiguous masks come
     * out from the highest to the lowest. With **a single** band they are several pixels per
     * element, which is the other packing: there the model is {@link MultiPixelPackedSampleModel}.
     *
     * @throws IllegalArgumentException if some parameter is not positive, if the bands do not fit
     *     in the type, or if the type is neither `byte`, `ushort` nor `int`
     */
    public static WritableRaster createPackedRaster(int dataType, int w, int h, int bands,
            int bitsPerBand, Point location) {
        if (bands <= 0) {
            throw new IllegalArgumentException("Number of bands (" + bands
                    + ") must be greater than 0");
        }
        if (bitsPerBand <= 0) {
            throw new IllegalArgumentException("Bits per band (" + bitsPerBand
                    + ") must be greater than 0");
        }
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        if (bands != 1) {
            int[] masks = new int[bands];
            int mask = (1 << bitsPerBand) - 1;
            int shift = (bands - 1) * bitsPerBand;
            if (shift + bitsPerBand > DataBuffer.getDataTypeSize(dataType)) {
                throw new IllegalArgumentException("bitsPerBand(" + bitsPerBand
                        + ") * bands is greater than data type size.");
            }
            for (int i = 0; i < bands; i++) {
                masks[i] = mask << shift;
                shift = shift - bitsPerBand;
            }
            return createPackedRaster(dataType, w, h, masks, location);
        }
        // A single band: several pixels per element. The size is rounded up because a row that does
        // not fill the last element takes it whole all the same.
        int perElement = DataBuffer.getDataTypeSize(dataType) / bitsPerBand;
        int elementsPerRow = (w + perElement - 1) / perElement;
        DataBuffer d;
        if (dataType == DataBuffer.TYPE_BYTE) {
            d = new DataBufferByte(elementsPerRow * h);
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            d = new DataBufferUShort(elementsPerRow * h);
        } else {
            d = new DataBufferInt(elementsPerRow * h);
        }
        return createPackedRaster(d, w, h, bitsPerBand, location);
    }

    /**
     * A raster packed by masks over the given buffer.
     *
     * @throws NullPointerException if the buffer is `null`
     * @throws RasterFormatException if the buffer has more than one bank
     * @throws IllegalArgumentException if the type is neither `byte`, `ushort` nor `int`
     */
    public static WritableRaster createPackedRaster(DataBuffer dataBuffer, int w, int h,
            int scanlineStride, int[] bandMasks, Point location) {
        if (dataBuffer == null) {
            throw new NullPointerException("DataBuffer cannot be null");
        }
        Point where = location;
        if (where == null) {
            where = new Point(0, 0);
        }
        int dataType = dataBuffer.getDataType();
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        if (dataBuffer.getNumBanks() != 1) {
            throw new RasterFormatException(
                    "DataBuffer for packed Rasters must only have 1 bank.");
        }
        SinglePixelPackedSampleModel sppsm = new SinglePixelPackedSampleModel(dataType, w, h,
                scanlineStride, bandMasks);
        return new WritableRaster(sppsm, dataBuffer, where);
    }

    /**
     * A raster of several pixels per element over the given buffer.
     *
     * @throws NullPointerException if the buffer is `null`
     * @throws RasterFormatException if the buffer has more than one bank
     * @throws IllegalArgumentException if the type is neither `byte`, `ushort` nor `int`
     */
    public static WritableRaster createPackedRaster(DataBuffer dataBuffer, int w, int h,
            int bitsPerPixel, Point location) {
        if (dataBuffer == null) {
            throw new NullPointerException("DataBuffer cannot be null");
        }
        Point where = location;
        if (where == null) {
            where = new Point(0, 0);
        }
        int dataType = dataBuffer.getDataType();
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        if (dataBuffer.getNumBanks() != 1) {
            throw new RasterFormatException(
                    "DataBuffer for packed Rasters must only have 1 bank.");
        }
        MultiPixelPackedSampleModel mppsm = new MultiPixelPackedSampleModel(dataType, w, h,
                bitsPerPixel);
        return new WritableRaster(mppsm, dataBuffer, where);
    }

    /**
     * A read-only raster over the given model and buffer.
     *
     * @throws NullPointerException if the model or the buffer is missing
     */
    public static Raster createRaster(SampleModel sm, DataBuffer db, Point location) {
        if (sm == null || db == null) {
            throw new NullPointerException("SampleModel and DataBuffer cannot be null");
        }
        Point where = location;
        if (where == null) {
            where = new Point(0, 0);
        }
        return new Raster(sm, db, where);
    }

    /**
     * A writable raster over the given model, with a new buffer.
     *
     * @throws NullPointerException if the model is missing
     */
    public static WritableRaster createWritableRaster(SampleModel sm, Point location) {
        if (sm == null) {
            throw new NullPointerException("SampleModel cannot be null");
        }
        Point where = location;
        if (where == null) {
            where = new Point(0, 0);
        }
        return new WritableRaster(sm, where);
    }

    /**
     * A writable raster over the given model and buffer.
     *
     * @throws NullPointerException if the model or the buffer is missing
     */
    public static WritableRaster createWritableRaster(SampleModel sm, DataBuffer db,
            Point location) {
        if (sm == null || db == null) {
            throw new NullPointerException("SampleModel and DataBuffer cannot be null");
        }
        Point where = location;
        if (where == null) {
            where = new Point(0, 0);
        }
        return new WritableRaster(sm, db, where);
    }

    // ---- geometry -----------------------------------------------------------------------------

    /** The raster this one is a crop of, or `null` if it is not one. */
    public Raster getParent() {
        return this.parent;
    }

    /** What has to be subtracted from a raster X to get the model's. */
    public final int getSampleModelTranslateX() {
        return this.sampleModelTranslateX;
    }

    /** What has to be subtracted from a raster Y to get the model's. */
    public final int getSampleModelTranslateY() {
        return this.sampleModelTranslateY;
    }

    /**
     * Another writable raster with the same model, at the origin and with data **of its own**.
     *
     * <p>It shares the layout, not the pixels.
     */
    public WritableRaster createCompatibleWritableRaster() {
        return new WritableRaster(this.sampleModel, new Point(0, 0));
    }

    /**
     * Like the previous one, of the size asked for.
     *
     * @throws RasterFormatException if the size is empty
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new RasterFormatException("negative " + (w <= 0 ? "width" : "height"));
        }
        SampleModel sm = this.sampleModel.createCompatibleSampleModel(w, h);
        return new WritableRaster(sm, new Point(0, 0));
    }

    /**
     * Like the previous one, of the size and at the position asked for.
     *
     * @throws RasterFormatException if the rectangle is empty
     */
    public WritableRaster createCompatibleWritableRaster(int x, int y, int w, int h) {
        WritableRaster ret = this.createCompatibleWritableRaster(w, h);
        return ret.createWritableChild(0, 0, w, h, x, y, null);
    }

    /**
     * Like the previous one, with the given rectangle.
     *
     * @throws NullPointerException if the rectangle is `null`
     */
    public WritableRaster createCompatibleWritableRaster(Rectangle rect) {
        if (rect == null) {
            throw new NullPointerException("Rect cannot be null");
        }
        return this.createCompatibleWritableRaster(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * The same raster moved to other coordinates, **over the same data**.
     *
     * @throws RasterFormatException if the new coordinates go past `int`
     */
    public Raster createTranslatedChild(int childMinX, int childMinY) {
        return this.createChild(this.minX, this.minY, this.width, this.height, childMinX, childMinY,
                null);
    }

    /**
     * A crop over the **same data**, optionally with fewer bands.
     *
     * <p>`bandList` chooses which bands the child sees and in what order; with `null` it sees them
     * all.
     *
     * @throws RasterFormatException if the rectangle asked for does not fall inside this one, or if
     *     the child's coordinates go past `int`
     */
    public Raster createChild(int parentX, int parentY, int width, int height, int childMinX,
            int childMinY, int[] bandList) {
        if (parentX < this.minX) {
            throw new RasterFormatException("parentX lies outside raster");
        }
        if (parentY < this.minY) {
            throw new RasterFormatException("parentY lies outside raster");
        }
        if (parentX + width < parentX || parentX + width > this.minX + this.width) {
            throw new RasterFormatException("(parentX + width) is outside raster");
        }
        if (parentY + height < parentY || parentY + height > this.minY + this.height) {
            throw new RasterFormatException("(parentY + height) is outside raster");
        }
        SampleModel subSampleModel;
        if (bandList == null) {
            subSampleModel = this.sampleModel;
        } else {
            subSampleModel = this.sampleModel.createSubsetSampleModel(bandList);
        }
        // The child is declared wherever it is asked for; the translation absorbs the difference so
        // that both coordinates go on falling on the same element of the buffer.
        int deltaX = childMinX - parentX;
        int deltaY = childMinY - parentY;
        return new Raster(subSampleModel, this.dataBuffer,
                new Rectangle(childMinX, childMinY, width, height),
                new Point(this.sampleModelTranslateX + deltaX,
                        this.sampleModelTranslateY + deltaY),
                this);
    }

    /** The rectangle it occupies. */
    public Rectangle getBounds() {
        return new Rectangle(this.minX, this.minY, this.width, this.height);
    }

    /** X coordinate of the top-left corner. */
    public final int getMinX() {
        return this.minX;
    }

    /** Y coordinate of the top-left corner. */
    public final int getMinY() {
        return this.minY;
    }

    /** Width, in pixels. */
    public final int getWidth() {
        return this.width;
    }

    /** Height, in pixels. */
    public final int getHeight() {
        return this.height;
    }

    /** How many bands each pixel has. */
    public final int getNumBands() {
        return this.numBands;
    }

    /** How many elements of the buffer a pixel takes. */
    public final int getNumDataElements() {
        return this.numDataElements;
    }

    /** The type a raw pixel is transferred with. */
    public final int getTransferType() {
        return this.sampleModel.getTransferType();
    }

    /**
     * The buffer with the numbers.
     *
     * <p>It is the real one, not a copy: writing into it changes the image, and that of every
     * raster that shares it.
     */
    public DataBuffer getDataBuffer() {
        return this.dataBuffer;
    }

    /** How the pixels are laid out. */
    public SampleModel getSampleModel() {
        return this.sampleModel;
    }

    // ---- reading -------------------------------------------------------------------------------

    /**
     * Checks that a point falls inside.
     *
     * @throws ArrayIndexOutOfBoundsException if it does not
     */
    private void checkPoint(int x, int y) {
        if (x < this.minX || y < this.minY || x >= this.minX + this.width
                || y >= this.minY + this.height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
    }

    /**
     * Checks that a rectangle falls inside.
     *
     * @throws ArrayIndexOutOfBoundsException if it does not
     */
    private void checkRect(int x, int y, int w, int h) {
        if (x < this.minX || y < this.minY || x + w > this.minX + this.width
                || y + h > this.minY + this.height || x + w < x || y + h < y) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
    }

    /**
     * The raw pixel, unpacked, in the type of {@link #getTransferType}.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public Object getDataElements(int x, int y, Object outData) {
        this.checkPoint(x, y);
        return this.sampleModel.getDataElements(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, outData, this.dataBuffer);
    }

    /**
     * The raw pixels of a rectangle.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public Object getDataElements(int x, int y, int w, int h, Object outData) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getDataElements(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, outData, this.dataBuffer);
    }

    /**
     * The bands of a pixel.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public int[] getPixel(int x, int y, int[] iArray) {
        this.checkPoint(x, y);
        return this.sampleModel.getPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, iArray, this.dataBuffer);
    }

    /**
     * The bands of a pixel, in `float`.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public float[] getPixel(int x, int y, float[] fArray) {
        this.checkPoint(x, y);
        return this.sampleModel.getPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, fArray, this.dataBuffer);
    }

    /**
     * The bands of a pixel, in `double`.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public double[] getPixel(int x, int y, double[] dArray) {
        this.checkPoint(x, y);
        return this.sampleModel.getPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, dArray, this.dataBuffer);
    }

    /**
     * The pixels of a rectangle, band by band.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public int[] getPixels(int x, int y, int w, int h, int[] iArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, iArray, this.dataBuffer);
    }

    /**
     * Like the previous one, in `float`.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public float[] getPixels(int x, int y, int w, int h, float[] fArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, fArray, this.dataBuffer);
    }

    /**
     * Like the previous one, in `double`.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public double[] getPixels(int x, int y, int w, int h, double[] dArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, dArray, this.dataBuffer);
    }

    /**
     * One band of a pixel.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public int getSample(int x, int y, int b) {
        this.checkPoint(x, y);
        return this.sampleModel.getSample(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, this.dataBuffer);
    }

    /**
     * One band of a pixel, in `float`.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public float getSampleFloat(int x, int y, int b) {
        this.checkPoint(x, y);
        return this.sampleModel.getSampleFloat(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, this.dataBuffer);
    }

    /**
     * One band of a pixel, in `double`.
     *
     * @throws ArrayIndexOutOfBoundsException if the point falls outside
     */
    public double getSampleDouble(int x, int y, int b) {
        this.checkPoint(x, y);
        return this.sampleModel.getSampleDouble(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, this.dataBuffer);
    }

    /**
     * One band over a rectangle.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public int[] getSamples(int x, int y, int w, int h, int b, int[] iArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, iArray, this.dataBuffer);
    }

    /**
     * Like the previous one, in `float`.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public float[] getSamples(int x, int y, int w, int h, int b, float[] fArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, fArray, this.dataBuffer);
    }

    /**
     * Like the previous one, in `double`.
     *
     * @throws ArrayIndexOutOfBoundsException if the rectangle goes outside
     */
    public double[] getSamples(int x, int y, int w, int h, int b, double[] dArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, dArray, this.dataBuffer);
    }
}
