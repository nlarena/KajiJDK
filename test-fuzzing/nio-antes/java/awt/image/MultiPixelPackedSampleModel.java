package java.awt.image;

/**
 * Several pixels inside **one** element of the buffer: the model of the 1-, 2- and 4-bit images.
 *
 * <p>It is the inverse of {@link SinglePixelPackedSampleModel}, where one pixel takes a whole
 * element. Here a `byte` can hold eight one-bit pixels — a black-and-white map — or two of four
 * bits. It has **a single band** by construction: if there were more than one they would not be
 * packed pixels but bit fields, which is the other model.
 *
 * <p>The sum is the same one twice, once for the element and once for the bit inside it:
 *
 * <pre>bit = x * pixelBitStride + dataBitOffset
 * element = y * scanlineStride + bit / bitsPerElement
 * shift = bit % bitsPerElement</pre>
 *
 * <p>The first pixel is counted **from the left**, that is, from the most significant bit: in a
 * byte with four two-bit pixels, pixel 0 is in bits 7-6. It is what the format says and it is the
 * opposite of what one would write by reflex.
 *
 * <p>`dataBitOffset` shifts the start of the first row. It serves to describe a crop whose left
 * edge falls in the middle of an element, without having to copy the image.
 */
public class MultiPixelPackedSampleModel extends SampleModel {

    private final int pixelBitStride;
    private final int scanlineStride;
    private final int dataBitOffset;
    private final int dataElementSize;
    private final int bitMask;

    /**
     * With the minimum row stride and no initial offset.
     *
     * @throws IllegalArgumentException if the type does not admit packing
     */
    public MultiPixelPackedSampleModel(int dataType, int w, int h, int numberOfBits) {
        this(dataType, w, h, numberOfBits,
                (w * numberOfBits + DataBuffer.getDataTypeSize(dataType) - 1)
                        / DataBuffer.getDataTypeSize(dataType),
                0);
    }

    /**
     * With everything given.
     *
     * @throws IllegalArgumentException if the type is neither `byte`, `ushort` nor `int`, if the
     *     bits per pixel do not divide the size of the element, or if some parameter is negative
     */
    public MultiPixelPackedSampleModel(int dataType, int w, int h, int numberOfBits,
            int scanlineStride, int dataBitOffset) {
        super(dataType, w, h, 1);
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        this.dataElementSize = DataBuffer.getDataTypeSize(dataType);
        if (numberOfBits <= 0 || numberOfBits > this.dataElementSize) {
            throw new RasterFormatException("Number of bits must be > 0 and <= "
                    + this.dataElementSize);
        }
        // That a pixel does not cross the boundary of an element is what makes it possible to read
        // it with a single access. Without that condition two elements would have to be joined, and
        // the format does not provide for it.
        if (this.dataElementSize % numberOfBits != 0) {
            throw new RasterFormatException("MultiPixelPackedSampleModel does not allow pixels to "
                    + "span data element boundaries");
        }
        if (scanlineStride < 0 || dataBitOffset < 0) {
            throw new IllegalArgumentException("Scanline stride and data bit offset must be >= 0");
        }
        this.pixelBitStride = numberOfBits;
        this.scanlineStride = scanlineStride;
        this.dataBitOffset = dataBitOffset;
        this.bitMask = (1 << numberOfBits) - 1;
    }

    /** Another one just like it of the size asked for, with no initial offset. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        return new MultiPixelPackedSampleModel(this.dataType, w, h, this.pixelBitStride);
    }

    /**
     * A buffer of the size needed.
     *
     * @throws IllegalArgumentException if the type is neither `byte`, `ushort` nor `int`
     */
    public DataBuffer createDataBuffer() {
        int size = (this.scanlineStride * (this.height - 1))
                + ((this.dataBitOffset + this.width * this.pixelBitStride
                        + this.dataElementSize - 1) / this.dataElementSize);
        if (this.dataType == DataBuffer.TYPE_BYTE) {
            return new DataBufferByte(size);
        }
        if (this.dataType == DataBuffer.TYPE_USHORT) {
            return new DataBufferUShort(size);
        }
        if (this.dataType == DataBuffer.TYPE_INT) {
            return new DataBufferInt(size);
        }
        throw new IllegalArgumentException("Unsupported data type " + this.dataType);
    }

    /** Always 1: the model has a single band. */
    public int getNumDataElements() {
        return 1;
    }

    /** The bits of the only band. */
    public int[] getSampleSize() {
        return new int[] { this.pixelBitStride };
    }

    /** The bits of that band. */
    public int getSampleSize(int band) {
        return this.pixelBitStride;
    }

    /** The element of the buffer where that pixel is. */
    public int getOffset(int x, int y) {
        return y * this.scanlineStride
                + (x * this.pixelBitStride + this.dataBitOffset) / this.dataElementSize;
    }

    /** The bit inside the element where that pixel starts, counted from the left. */
    public int getBitOffset(int x) {
        return (x * this.pixelBitStride + this.dataBitOffset) % this.dataElementSize;
    }

    /** The row stride, in elements. */
    public int getScanlineStride() {
        return this.scanlineStride;
    }

    /** How many bits a pixel takes. */
    public int getPixelBitStride() {
        return this.pixelBitStride;
    }

    /** How many bits are skipped at the start. */
    public int getDataBitOffset() {
        return this.dataBitOffset;
    }

    /**
     * The type a pixel is transferred with.
     *
     * <p>It is not the buffer's: what travels here is **one pixel**, which fits in the smallest
     * type that holds it. A one-bit image stored in `int` transfers in `byte`.
     */
    public int getTransferType() {
        if (this.pixelBitStride > 16) {
            return DataBuffer.TYPE_INT;
        }
        if (this.pixelBitStride > 8) {
            return DataBuffer.TYPE_USHORT;
        }
        return DataBuffer.TYPE_BYTE;
    }

    /**
     * One with that band.
     *
     * @throws RasterFormatException if more than one band is asked for, or one that is not band 0
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
        if (bands != null && (bands.length != 1 || bands[0] != 0)) {
            throw new RasterFormatException("MultiPixelPackedSampleModel has only one band");
        }
        return this.createCompatibleSampleModel(this.width, this.height);
    }

    public int getSample(int x, int y, int b, DataBuffer data) {
        // The shift is counted from the left: pixel 0 is in the highest bits.
        int shift = this.dataElementSize - this.getBitOffset(x) - this.pixelBitStride;
        return (data.getElem(this.getOffset(x, y)) >> shift) & this.bitMask;
    }

    public void setSample(int x, int y, int b, int s, DataBuffer data) {
        int off = this.getOffset(x, y);
        int shift = this.dataElementSize - this.getBitOffset(x) - this.pixelBitStride;
        int v = data.getElem(off);
        v = v & ~(this.bitMask << shift);
        v = v | ((s & this.bitMask) << shift);
        data.setElem(off, v);
    }

    /**
     * The raw pixel, in the type of {@link #getTransferType}.
     *
     * <p>It comes **unpacked** already: a two-bit pixel arrives as a byte with value 0..3, not as
     * the byte of the buffer with the other three pixels inside. That is the difference from {@link
     * SinglePixelPackedSampleModel}, where the raw element is the whole packed pixel.
     */
    public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
        int v = this.getSample(x, y, 0, data);
        int type = this.getTransferType();
        if (type == DataBuffer.TYPE_BYTE) {
            byte[] out = obj == null ? new byte[1] : (byte[]) obj;
            out[0] = (byte) v;
            return out;
        }
        if (type == DataBuffer.TYPE_USHORT) {
            short[] out = obj == null ? new short[1] : (short[]) obj;
            out[0] = (short) v;
            return out;
        }
        int[] out = obj == null ? new int[1] : (int[]) obj;
        out[0] = v;
        return out;
    }

    /** Writes the raw pixel. */
    public void setDataElements(int x, int y, Object obj, DataBuffer data) {
        int type = this.getTransferType();
        int v;
        if (type == DataBuffer.TYPE_BYTE) {
            v = ((byte[]) obj)[0] & 0xFF;
        } else if (type == DataBuffer.TYPE_USHORT) {
            v = ((short[]) obj)[0] & 0xFFFF;
        } else {
            v = ((int[]) obj)[0];
        }
        this.setSample(x, y, 0, v, data);
    }

    /** The only band of the pixel. */
    public int[] getPixel(int x, int y, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[1] : iArray;
        out[0] = this.getSample(x, y, 0, data);
        return out;
    }

    /** Writes the only band of the pixel. */
    public void setPixel(int x, int y, int[] iArray, DataBuffer data) {
        this.setSample(x, y, 0, iArray[0], data);
    }

    /** Equality by size, type and the three packing parameters. */
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        MultiPixelPackedSampleModel that = (MultiPixelPackedSampleModel) o;
        return this.width == that.width && this.height == that.height
                && this.numBands == that.numBands && this.dataType == that.dataType
                && this.pixelBitStride == that.pixelBitStride
                && this.bitMask == that.bitMask
                && this.scanlineStride == that.scanlineStride
                && this.dataBitOffset == that.dataBitOffset;
    }

    public int hashCode() {
        int h = this.width;
        h = h * 31 + this.height;
        h = h * 31 + this.numBands;
        h = h * 31 + this.dataType;
        h = h * 31 + this.pixelBitStride;
        h = h * 31 + this.bitMask;
        h = h * 31 + this.scanlineStride;
        h = h * 31 + this.dataBitOffset;
        return h;
    }
}
