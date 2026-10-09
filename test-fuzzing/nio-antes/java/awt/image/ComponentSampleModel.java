package java.awt.image;

/**
 * The model where **each band is one whole number of the buffer**, unpacked.
 *
 * <p>It is the most general of the five and the one that covers almost everything that is not a
 * packed pixel. Its whole idea fits in one formula: the element of band `b` of pixel `(x,y)` is at
 *
 * <pre>y * scanlineStride + x * pixelStride + bandOffsets[b]</pre>
 *
 * <p>inside bank `bankIndices[b]`. The four parameters of that sum are what makes the same class
 * describe very different formats:
 *
 * <ul>
 * <li><b>Interleaved</b> (RGBRGBRGB…): one bank, `pixelStride` 3, `bandOffsets` {0,1,2}.</li>
 * <li><b>By planes</b> (RRR…GGG…BBB…): three banks, `pixelStride` 1, `bandOffsets` {0,0,0}.</li>
 * <li><b>With row padding</b>: `scanlineStride` greater than `width * pixelStride`, which is what
 *     happens when each row is aligned to a boundary.</li>
 * <li><b>A crop</b>: the same data with `bandOffsets` shifted, copying nothing.</li>
 * <li><b>With the bands turned round</b> (BGR): `bandOffsets` {2,1,0}.</li>
 * </ul>
 *
 * <p>That all of it comes out of a single formula is the reason this class exists, and also the
 * reason its five protected fields are part of the contract: a subclass needs them to do the same
 * sum faster.
 */
public class ComponentSampleModel extends SampleModel {

    /** Where each band starts inside its bank. */
    protected int[] bandOffsets;

    /** Which bank each band is in. */
    protected int[] bankIndices;

    /** How many bands there are. */
    protected int numBands;

    /** How many banks it uses. */
    protected int numBanks;

    /** How many elements there are between the start of one row and that of the next. */
    protected int scanlineStride;

    /** How many elements there are between one pixel and the one beside it. */
    protected int pixelStride;

    /**
     * Every band in bank 0.
     *
     * @throws IllegalArgumentException if the strides are negative or there are no offsets
     */
    public ComponentSampleModel(int dataType, int w, int h, int pixelStride, int scanlineStride,
            int[] bandOffsets) {
        super(dataType, w, h, bandOffsets.length);
        if (pixelStride < 0) {
            throw new IllegalArgumentException("Pixel stride must be >= 0");
        }
        if (scanlineStride < 0) {
            throw new IllegalArgumentException("Scanline stride must be >= 0");
        }
        this.pixelStride = pixelStride;
        this.scanlineStride = scanlineStride;
        this.bandOffsets = copyOf(bandOffsets);
        this.numBands = bandOffsets.length;
        this.numBanks = 1;
        this.bankIndices = new int[this.numBands];
        for (int i = 0; i < this.numBands; i++) {
            this.bankIndices[i] = 0;
        }
    }

    /**
     * Each band in whichever bank is given.
     *
     * @throws IllegalArgumentException if the strides are negative, or if there is a different
     *     number of banks than of offsets
     */
    public ComponentSampleModel(int dataType, int w, int h, int pixelStride, int scanlineStride,
            int[] bankIndices, int[] bandOffsets) {
        super(dataType, w, h, bandOffsets.length);
        if (pixelStride < 0) {
            throw new IllegalArgumentException("Pixel stride must be >= 0");
        }
        if (scanlineStride < 0) {
            throw new IllegalArgumentException("Scanline stride must be >= 0");
        }
        if (bankIndices.length != bandOffsets.length) {
            throw new IllegalArgumentException(
                    "Length of bandOffsets must equal length of bankIndices");
        }
        this.pixelStride = pixelStride;
        this.scanlineStride = scanlineStride;
        this.bandOffsets = copyOf(bandOffsets);
        this.bankIndices = copyOf(bankIndices);
        this.numBands = bandOffsets.length;
        int max = 0;
        for (int i = 0; i < bankIndices.length; i++) {
            if (bankIndices[i] < 0) {
                throw new IllegalArgumentException("Index of bank must be >= 0");
            }
            if (bankIndices[i] > max) {
                max = bankIndices[i];
            }
        }
        this.numBanks = max + 1;
    }

    private static int[] copyOf(int[] src) {
        int[] out = new int[src.length];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    /**
     * A model just like it but of another size. The row stride is worked out again for the new
     * width.
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        int[] offsets = copyOf(this.bandOffsets);
        return new ComponentSampleModel(this.dataType, w, h, this.pixelStride,
                this.pixelStride * w, this.bankIndices, offsets);
    }

    /**
     * A model with only those bands, **over the same data**.
     *
     * @throws RasterFormatException if some band does not exist
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
        int[] indices = new int[bands.length];
        int[] offsets = new int[bands.length];
        for (int i = 0; i < bands.length; i++) {
            if (bands[i] < 0 || bands[i] >= this.numBands) {
                throw new RasterFormatException("Band " + bands[i] + " does not exist");
            }
            indices[i] = this.bankIndices[bands[i]];
            offsets[i] = this.bandOffsets[bands[i]];
        }
        return new ComponentSampleModel(this.dataType, this.width, this.height, this.pixelStride,
                this.scanlineStride, indices, offsets);
    }

    /**
     * A buffer of the size this model needs.
     *
     * <p>The size is not `width * height * bands`: it is what the last row takes **plus** the
     * biggest offset, because with row padding or with shifted offsets there are elements the model
     * never touches and that have to exist all the same.
     *
     * @throws IllegalArgumentException if the data type is not one of the six
     */
    public DataBuffer createDataBuffer() {
        int max = 0;
        for (int i = 0; i < this.bandOffsets.length; i++) {
            if (this.bandOffsets[i] > max) {
                max = this.bandOffsets[i];
            }
        }
        int size = (this.height - 1) * this.scanlineStride
                + (this.width - 1) * this.pixelStride + max + 1;
        if (this.dataType == DataBuffer.TYPE_BYTE) {
            return new DataBufferByte(size, this.numBanks);
        }
        if (this.dataType == DataBuffer.TYPE_USHORT) {
            return new DataBufferUShort(size, this.numBanks);
        }
        if (this.dataType == DataBuffer.TYPE_SHORT) {
            return new DataBufferShort(size, this.numBanks);
        }
        if (this.dataType == DataBuffer.TYPE_INT) {
            return new DataBufferInt(size, this.numBanks);
        }
        if (this.dataType == DataBuffer.TYPE_FLOAT) {
            return new DataBufferFloat(size, this.numBanks);
        }
        if (this.dataType == DataBuffer.TYPE_DOUBLE) {
            return new DataBufferDouble(size, this.numBanks);
        }
        throw new IllegalArgumentException("Unsupported dataType: " + this.dataType);
    }

    /** The offset of band 0 of that pixel. See the formula of the class. */
    public int getOffset(int x, int y) {
        return y * this.scanlineStride + x * this.pixelStride + this.bandOffsets[0];
    }

    /** The offset of that band of that pixel. */
    public int getOffset(int x, int y, int b) {
        return y * this.scanlineStride + x * this.pixelStride + this.bandOffsets[b];
    }

    /** The bits of each band: those of the buffer's type, because nothing is packed here. */
    public final int[] getSampleSize() {
        int bits = DataBuffer.getDataTypeSize(this.dataType);
        int[] out = new int[this.numBands];
        for (int i = 0; i < this.numBands; i++) {
            out[i] = bits;
        }
        return out;
    }

    /** The bits of that band. */
    public final int getSampleSize(int band) {
        return DataBuffer.getDataTypeSize(this.dataType);
    }

    /** Which bank each band is in. */
    public final int[] getBankIndices() {
        return copyOf(this.bankIndices);
    }

    /** Where each band starts. */
    public final int[] getBandOffsets() {
        return copyOf(this.bandOffsets);
    }

    /** The row stride. */
    public final int getScanlineStride() {
        return this.scanlineStride;
    }

    /** The pixel stride. */
    public final int getPixelStride() {
        return this.pixelStride;
    }

    /** One element per band: there is no packing here. */
    public final int getNumDataElements() {
        return this.numBands;
    }

    /**
     * The raw representation of a pixel: one element per band, in the buffer's type.
     *
     * @throws IllegalArgumentException if the data type is not one of the six
     */
    public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
        int type = this.getTransferType();
        if (type == DataBuffer.TYPE_BYTE) {
            byte[] out = obj == null ? new byte[this.numBands] : (byte[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                out[i] = (byte) data.getElem(this.bankIndices[i], this.getOffset(x, y, i));
            }
            return out;
        }
        if (type == DataBuffer.TYPE_USHORT || type == DataBuffer.TYPE_SHORT) {
            short[] out = obj == null ? new short[this.numBands] : (short[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                out[i] = (short) data.getElem(this.bankIndices[i], this.getOffset(x, y, i));
            }
            return out;
        }
        if (type == DataBuffer.TYPE_INT) {
            int[] out = obj == null ? new int[this.numBands] : (int[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                out[i] = data.getElem(this.bankIndices[i], this.getOffset(x, y, i));
            }
            return out;
        }
        if (type == DataBuffer.TYPE_FLOAT) {
            float[] out = obj == null ? new float[this.numBands] : (float[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                out[i] = data.getElemFloat(this.bankIndices[i], this.getOffset(x, y, i));
            }
            return out;
        }
        if (type == DataBuffer.TYPE_DOUBLE) {
            double[] out = obj == null ? new double[this.numBands] : (double[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                out[i] = data.getElemDouble(this.bankIndices[i], this.getOffset(x, y, i));
            }
            return out;
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    /**
     * Writes the raw representation of a pixel.
     *
     * @throws IllegalArgumentException if the data type is not one of the six
     */
    public void setDataElements(int x, int y, Object obj, DataBuffer data) {
        int type = this.getTransferType();
        if (type == DataBuffer.TYPE_BYTE) {
            byte[] src = (byte[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                data.setElem(this.bankIndices[i], this.getOffset(x, y, i), src[i] & 0xFF);
            }
            return;
        }
        if (type == DataBuffer.TYPE_USHORT || type == DataBuffer.TYPE_SHORT) {
            short[] src = (short[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                data.setElem(this.bankIndices[i], this.getOffset(x, y, i), src[i] & 0xFFFF);
            }
            return;
        }
        if (type == DataBuffer.TYPE_INT) {
            int[] src = (int[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                data.setElem(this.bankIndices[i], this.getOffset(x, y, i), src[i]);
            }
            return;
        }
        if (type == DataBuffer.TYPE_FLOAT) {
            float[] src = (float[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                data.setElemFloat(this.bankIndices[i], this.getOffset(x, y, i), src[i]);
            }
            return;
        }
        if (type == DataBuffer.TYPE_DOUBLE) {
            double[] src = (double[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                data.setElemDouble(this.bankIndices[i], this.getOffset(x, y, i), src[i]);
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    public int getSample(int x, int y, int b, DataBuffer data) {
        return data.getElem(this.bankIndices[b], this.getOffset(x, y, b));
    }

    public float getSampleFloat(int x, int y, int b, DataBuffer data) {
        return data.getElemFloat(this.bankIndices[b], this.getOffset(x, y, b));
    }

    public double getSampleDouble(int x, int y, int b, DataBuffer data) {
        return data.getElemDouble(this.bankIndices[b], this.getOffset(x, y, b));
    }

    /** Writes every band of a pixel. */
    public void setPixel(int x, int y, int[] iArray, DataBuffer data) {
        for (int i = 0; i < this.numBands; i++) {
            data.setElem(this.bankIndices[i], this.getOffset(x, y, i), iArray[i]);
        }
    }

    public void setSample(int x, int y, int b, int s, DataBuffer data) {
        data.setElem(this.bankIndices[b], this.getOffset(x, y, b), s);
    }

    public void setSample(int x, int y, int b, float s, DataBuffer data) {
        data.setElemFloat(this.bankIndices[b], this.getOffset(x, y, b), s);
    }

    public void setSample(int x, int y, int b, double s, DataBuffer data) {
        data.setElemDouble(this.bankIndices[b], this.getOffset(x, y, b), s);
    }

    /**
     * Equality by **everything** that defines the model, the class included.
     *
     * <p>The exact-class check is not laziness: a {@link BandedSampleModel} and a
     * `ComponentSampleModel` with the same numbers describe the same format but behave differently
     * in `createCompatibleSampleModel`, so treating them as equal would make a copy come out with
     * another format.
     */
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        ComponentSampleModel that = (ComponentSampleModel) o;
        if (this.width != that.width || this.height != that.height
                || this.numBands != that.numBands || this.dataType != that.dataType
                || this.pixelStride != that.pixelStride
                || this.scanlineStride != that.scanlineStride
                || this.numBanks != that.numBanks) {
            return false;
        }
        for (int i = 0; i < this.numBands; i++) {
            if (this.bandOffsets[i] != that.bandOffsets[i]
                    || this.bankIndices[i] != that.bankIndices[i]) {
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
        h = h * 31 + this.pixelStride;
        h = h * 31 + this.scanlineStride;
        for (int i = 0; i < this.numBands; i++) {
            h = h * 31 + this.bandOffsets[i];
            h = h * 31 + this.bankIndices[i];
        }
        return h;
    }
}
