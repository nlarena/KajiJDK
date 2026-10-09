package java.awt.image;

/**
 * One whole pixel in **a single** element of the buffer, with its bands in bit fields.

  * <p>It is the model of a screen image: one `int` per pixel with the alpha, the red, the green and
  * the blue in their eight bits each. The bands are declared by their **masks**, and out of the
  * mask comes everything else — how many bits the band uses and how far it has to be shifted.
  *
  * <p>That the mask is the parameter, and not the pair (offset, width), is what makes irregular
  * formats just as easy to declare: a 5-6-5 of 16 bits is the masks `0xF800, 0x07E0, 0x001F` and
  * nothing else needs saying.
  *
  * <p><strong>The masks cannot overlap</strong> and the constructor does not check it, just like
  * the JDK: two bands over the same bits would produce an image where writing one changes the
  * other, and detecting it would cost comparing every pair in a constructor that is called once per
  * image.
  */
public class SinglePixelPackedSampleModel extends SampleModel {

    private final int[] bitMasks;
    private final int[] bitOffsets;
    private final int[] bitSizes;
    private final int scanlineStride;

    /** One pixel per element, with no row padding. */
    public SinglePixelPackedSampleModel(int dataType, int w, int h, int[] bitMasks) {
        this(dataType, w, h, w, bitMasks);
    }

    /**
     * With the row stride given.
     *
     * @throws IllegalArgumentException if the type does not admit packing --only `byte`, `ushort`
     *     and `int` do-- or if the row stride is negative
     */
    public SinglePixelPackedSampleModel(int dataType, int w, int h, int scanlineStride,
            int[] bitMasks) {
        super(dataType, w, h, bitMasks.length);
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        if (scanlineStride < 0) {
            throw new IllegalArgumentException("Scanline stride must be >= 0");
        }
        this.scanlineStride = scanlineStride;
        this.bitMasks = new int[bitMasks.length];
        this.bitOffsets = new int[bitMasks.length];
        this.bitSizes = new int[bitMasks.length];
        for (int i = 0; i < bitMasks.length; i++) {
            int mask = bitMasks[i];
            this.bitMasks[i] = mask;
            // The shift is how many zeros there are to the right of the mask, and the size how many
            // ones it has once shifted. Both come out of the same pass.
            int off = 0;
            int m = mask;
            if (m != 0) {
                while ((m & 1) == 0) {
                    m = m >>> 1;
                    off = off + 1;
                }
            }
            int bits = 0;
            while ((m & 1) == 1) {
                m = m >>> 1;
                bits = bits + 1;
            }
            this.bitOffsets[i] = off;
            this.bitSizes[i] = bits;
        }
    }

    /** Always 1: the whole pixel fits in one element. */
    public int getNumDataElements() {
        return 1;
    }

    /** Another one just like it of the size asked for. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        return new SinglePixelPackedSampleModel(this.dataType, w, h, w, this.getBitMasks());
    }

    /**
     * A buffer of the size needed.
     *
     * @throws IllegalArgumentException if the type is neither `byte`, `ushort` nor `int`
     */
    public DataBuffer createDataBuffer() {
        int size = (this.height - 1) * this.scanlineStride + this.width;
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

    /** How many bits each band uses, according to its mask. */
    public int[] getSampleSize() {
        int[] out = new int[this.bitSizes.length];
        System.arraycopy(this.bitSizes, 0, out, 0, this.bitSizes.length);
        return out;
    }

    /** How many bits that band uses. */
    public int getSampleSize(int band) {
        return this.bitSizes[band];
    }

    /** The element of the buffer where that pixel is. */
    public int getOffset(int x, int y) {
        return y * this.scanlineStride + x;
    }

    /** How many bits each band has to be shifted to bring it to the right. */
    public int[] getBitOffsets() {
        int[] out = new int[this.bitOffsets.length];
        System.arraycopy(this.bitOffsets, 0, out, 0, this.bitOffsets.length);
        return out;
    }

    /** The masks of each band. */
    public int[] getBitMasks() {
        int[] out = new int[this.bitMasks.length];
        System.arraycopy(this.bitMasks, 0, out, 0, this.bitMasks.length);
        return out;
    }

    /** The row stride. */
    public int getScanlineStride() {
        return this.scanlineStride;
    }

    /**
     * One with only those bands, over the same data.
     *
     * @throws RasterFormatException if some band does not exist
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
        int[] masks = new int[bands.length];
        for (int i = 0; i < bands.length; i++) {
            if (bands[i] < 0 || bands[i] >= this.numBands) {
                throw new RasterFormatException("Band " + bands[i] + " does not exist");
            }
            masks[i] = this.bitMasks[bands[i]];
        }
        return new SinglePixelPackedSampleModel(this.dataType, this.width, this.height,
                this.scanlineStride, masks);
    }

    /**
     * The raw element of the pixel: **one** value with every band packed in.
     *
     * @throws IllegalArgumentException if the type is neither `byte`, `ushort` nor `int`
     */
    public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
        int v = data.getElem(this.getOffset(x, y));
        if (this.dataType == DataBuffer.TYPE_BYTE) {
            byte[] out = obj == null ? new byte[1] : (byte[]) obj;
            out[0] = (byte) v;
            return out;
        }
        if (this.dataType == DataBuffer.TYPE_USHORT) {
            short[] out = obj == null ? new short[1] : (short[]) obj;
            out[0] = (short) v;
            return out;
        }
        if (this.dataType == DataBuffer.TYPE_INT) {
            int[] out = obj == null ? new int[1] : (int[]) obj;
            out[0] = v;
            return out;
        }
        throw new IllegalArgumentException("Unsupported data type " + this.dataType);
    }

    /** Writes the raw element of the pixel. */
    public void setDataElements(int x, int y, Object obj, DataBuffer data) {
        if (this.dataType == DataBuffer.TYPE_BYTE) {
            data.setElem(this.getOffset(x, y), ((byte[]) obj)[0] & 0xFF);
            return;
        }
        if (this.dataType == DataBuffer.TYPE_USHORT) {
            data.setElem(this.getOffset(x, y), ((short[]) obj)[0] & 0xFFFF);
            return;
        }
        if (this.dataType == DataBuffer.TYPE_INT) {
            data.setElem(this.getOffset(x, y), ((int[]) obj)[0]);
            return;
        }
        throw new IllegalArgumentException("Unsupported data type " + this.dataType);
    }

    /**
     * The bands of a pixel, unpacked.
     *
     * <p>It is overridden because here the whole pixel is read **once** and taken apart afterwards;
     * the inherited version would read the same element of the buffer once per band.
     */
    public int[] getPixel(int x, int y, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[this.numBands] : iArray;
        int v = data.getElem(this.getOffset(x, y));
        for (int i = 0; i < this.numBands; i++) {
            out[i] = (v & this.bitMasks[i]) >>> this.bitOffsets[i];
        }
        return out;
    }

    /** Like the previous one, for a rectangle. */
    public int[] getPixels(int x, int y, int w, int h, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[w * h * this.numBands] : iArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                int v = data.getElem(this.getOffset(i, j));
                for (int b = 0; b < this.numBands; b++) {
                    out[k] = (v & this.bitMasks[b]) >>> this.bitOffsets[b];
                    k = k + 1;
                }
            }
        }
        return out;
    }

    public int getSample(int x, int y, int b, DataBuffer data) {
        return (data.getElem(this.getOffset(x, y)) & this.bitMasks[b]) >>> this.bitOffsets[b];
    }

    /** The values of one band over a rectangle. */
    public int[] getSamples(int x, int y, int w, int h, int b, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[w * h] : iArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                out[k] = this.getSample(i, j, b, data);
                k = k + 1;
            }
        }
        return out;
    }

    /**
     * Writes every band of a pixel.
     *
     * <p>The whole value is assembled and written once. Each band is masked with its own before
     * being joined in: a value that goes past its field is trimmed instead of stepping on the band
     * beside it, which is what would happen without the `&`.
     */
    public void setPixel(int x, int y, int[] iArray, DataBuffer data) {
        int off = this.getOffset(x, y);
        int v = data.getElem(off);
        for (int i = 0; i < this.numBands; i++) {
            v = v & ~this.bitMasks[i];
            v = v | ((iArray[i] << this.bitOffsets[i]) & this.bitMasks[i]);
        }
        data.setElem(off, v);
    }

    /** Like the previous one, for a rectangle. */
    public void setPixels(int x, int y, int w, int h, int[] iArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                int[] one = new int[this.numBands];
                for (int b = 0; b < this.numBands; b++) {
                    one[b] = iArray[k];
                    k = k + 1;
                }
                this.setPixel(i, j, one, data);
            }
        }
    }

    public void setSample(int x, int y, int b, int s, DataBuffer data) {
        int off = this.getOffset(x, y);
        int v = data.getElem(off);
        v = v & ~this.bitMasks[b];
        v = v | ((s << this.bitOffsets[b]) & this.bitMasks[b]);
        data.setElem(off, v);
    }

    /** Writes the values of one band over a rectangle. */
    public void setSamples(int x, int y, int w, int h, int b, int[] iArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                this.setSample(i, j, b, iArray[k], data);
                k = k + 1;
            }
        }
    }

    /** Equality by size, type, row stride and masks. */
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        SinglePixelPackedSampleModel that = (SinglePixelPackedSampleModel) o;
        if (this.width != that.width || this.height != that.height
                || this.numBands != that.numBands || this.dataType != that.dataType
                || this.scanlineStride != that.scanlineStride) {
            return false;
        }
        for (int i = 0; i < this.bitMasks.length; i++) {
            if (this.bitMasks[i] != that.bitMasks[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = this.width;
        h = h * 31 + this.height;
        h = h * 31 + this.numBands;
        h = h * 31 + this.dataType;
        h = h * 31 + this.scanlineStride;
        for (int i = 0; i < this.bitMasks.length; i++) {
            h = h * 31 + this.bitMasks[i];
        }
        return h;
    }
}
