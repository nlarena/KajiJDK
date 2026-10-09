package java.awt.image;

/**
 * What a pixel is inside a {@link DataBuffer}: how many bands it has and where each one is.
 *
 * <p>It is the second of the three layers of `java.awt.image` and the one that gives the first one
 * its meaning. The buffer is an array of numbers with no structure; this model says that number 17
 * is the green band of pixel (5,2). What it does **not** say is what colour that green is — that is
 * said by a {@link ColorModel}.
 *
 * <p>The separation is what makes it possible to read the same memory in two ways without copying
 * it, and it is what makes a crop of an image be a new model over the same buffer instead of a
 * copy.
 *
 * <h2>Samples versus data elements</h2>
 *
 * <p>There are two families of accessors and confusing them is the classic mistake:
 *
 * <ul>
 * <li>The **samples** ({@code getSample}, {@code getPixel}) give the values of each band already
 *     unpacked, always as `int`, `float` or `double`. It is what one wants for doing
 *     arithmetic.</li>
 * <li>The **data elements** ({@code getDataElements}) give the raw representation, in the buffer's
 *     type and with whatever packing the model uses. It is what one wants for **copying** from one
 *     image to another of the same type, because it neither unpacks nor packs again.</li>
 * </ul>
 *
 * <p>Hence `getDataElements` returning `Object`: the real type is `byte[]`, `short[]`, `int[]`,
 * `float[]` or `double[]` according to {@link #getTransferType}, and there is no common supertype
 * of arrays of primitives.
 *
 * <h2>What a subclass implements</h2>
 *
 * <p>Six members only: {@code getSample}, {@code setSample}, the two single-pixel forms of
 * {@code getDataElements}/{@code setDataElements}, and the three `create...` methods plus
 * {@code getSampleSize}. All the rest of this class is written in terms of those — the rectangles
 * are loops over the pixel, and the `float`/`double` variants are conversions. A subclass can
 * override them to go faster, but it does not have to in order to be correct.
 */
public abstract class SampleModel {

    /** The width in pixels. */
    protected int width;

    /** The height in pixels. */
    protected int height;

    /** How many bands each pixel has. */
    protected int numBands;

    /** The type of the buffer's data: a `DataBuffer.TYPE_` constant. */
    protected int dataType;

    /**
     * A model of `w` by `h` with `numBands` bands over data of that type.
     *
     * @throws IllegalArgumentException if the width or the height are not positive, if their
     *     product goes past `Integer.MAX_VALUE`, or if the type is not one of the six
     */
    public SampleModel(int dataType, int w, int h, int numBands) {
        long size = (long) w * h;
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") must be > 0");
        }
        if (size >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Dimensions (width=" + w + " height=" + h
                    + ") are too large");
        }
        if (dataType < DataBuffer.TYPE_BYTE
                || (dataType > DataBuffer.TYPE_DOUBLE && dataType != DataBuffer.TYPE_UNDEFINED)) {
            throw new IllegalArgumentException("Unsupported dataType: " + dataType);
        }
        if (numBands <= 0) {
            throw new IllegalArgumentException("Number of bands must be > 0");
        }
        this.dataType = dataType;
        this.width = w;
        this.height = h;
        this.numBands = numBands;
    }

    /** The width. */
    public final int getWidth() {
        return this.width;
    }

    /** The height. */
    public final int getHeight() {
        return this.height;
    }

    /** How many bands. */
    public final int getNumBands() {
        return this.numBands;
    }

    /** How many elements of the buffer a pixel takes. See the note about data elements. */
    public abstract int getNumDataElements();

    /** The type of the buffer's data. */
    public final int getDataType() {
        return this.dataType;
    }

    /**
     * The type of the arrays of {@link #getDataElements}.
     *
     * <p>By default it is the same as the buffer's, and it almost always is. It is kept apart
     * because a packed model can transfer in a type wider than the one it stores in -- for
     * instance, storing in `byte[]` and transferring in `int` when a pixel does not fit in a byte.
     */
    public int getTransferType() {
        return this.dataType;
    }

    // ---- one pixel, by samples ------------------------------------------------------------

    /**
     * The bands of that pixel.
     *
     * @param iArray where to leave them, or null to have one reserved
     * @throws ArrayIndexOutOfBoundsException if the pixel falls outside the model
     */
    public int[] getPixel(int x, int y, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[this.numBands] : iArray;
        for (int i = 0; i < this.numBands; i++) {
            out[i] = this.getSample(x, y, i, data);
        }
        return out;
    }

    /** Like {@link #getPixel(int, int, int[], DataBuffer)}, in `float`. */
    public float[] getPixel(int x, int y, float[] fArray, DataBuffer data) {
        float[] out = fArray == null ? new float[this.numBands] : fArray;
        for (int i = 0; i < this.numBands; i++) {
            out[i] = this.getSampleFloat(x, y, i, data);
        }
        return out;
    }

    /** Like {@link #getPixel(int, int, int[], DataBuffer)}, in `double`. */
    public double[] getPixel(int x, int y, double[] dArray, DataBuffer data) {
        double[] out = dArray == null ? new double[this.numBands] : dArray;
        for (int i = 0; i < this.numBands; i++) {
            out[i] = this.getSampleDouble(x, y, i, data);
        }
        return out;
    }

    /** Writes the bands of that pixel. */
    public void setPixel(int x, int y, int[] iArray, DataBuffer data) {
        for (int i = 0; i < this.numBands; i++) {
            this.setSample(x, y, i, iArray[i], data);
        }
    }

    /** Writes the bands of that pixel, from `float`. */
    public void setPixel(int x, int y, float[] fArray, DataBuffer data) {
        for (int i = 0; i < this.numBands; i++) {
            this.setSample(x, y, i, fArray[i], data);
        }
    }

    /** Writes the bands of that pixel, from `double`. */
    public void setPixel(int x, int y, double[] dArray, DataBuffer data) {
        for (int i = 0; i < this.numBands; i++) {
            this.setSample(x, y, i, dArray[i], data);
        }
    }

    // ---- one rectangle, by samples -------------------------------------------------------
    //
    // The order is by rows and within each pixel by band, which is the one any code that walks the
    // array linearly expects.

    /** The bands of every pixel of that rectangle. */
    public int[] getPixels(int x, int y, int w, int h, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[w * h * this.numBands] : iArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    out[k] = this.getSample(i, j, b, data);
                    k = k + 1;
                }
            }
        }
        return out;
    }

    /** Like the previous one, in `float`. */
    public float[] getPixels(int x, int y, int w, int h, float[] fArray, DataBuffer data) {
        float[] out = fArray == null ? new float[w * h * this.numBands] : fArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    out[k] = this.getSampleFloat(i, j, b, data);
                    k = k + 1;
                }
            }
        }
        return out;
    }

    /** Like the previous one, in `double`. */
    public double[] getPixels(int x, int y, int w, int h, double[] dArray, DataBuffer data) {
        double[] out = dArray == null ? new double[w * h * this.numBands] : dArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    out[k] = this.getSampleDouble(i, j, b, data);
                    k = k + 1;
                }
            }
        }
        return out;
    }

    /** Writes the bands of every pixel of that rectangle. */
    public void setPixels(int x, int y, int w, int h, int[] iArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    this.setSample(i, j, b, iArray[k], data);
                    k = k + 1;
                }
            }
        }
    }

    /** Like the previous one, from `float`. */
    public void setPixels(int x, int y, int w, int h, float[] fArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    this.setSample(i, j, b, fArray[k], data);
                    k = k + 1;
                }
            }
        }
    }

    /** Like the previous one, from `double`. */
    public void setPixels(int x, int y, int w, int h, double[] dArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                for (int b = 0; b < this.numBands; b++) {
                    this.setSample(i, j, b, dArray[k], data);
                    k = k + 1;
                }
            }
        }
    }

    // ---- one band -------------------------------------------------------------------------

    /**
     * The value of one band of one pixel.
     *
     * @throws ArrayIndexOutOfBoundsException if the pixel or the band fall outside
     */
    public abstract int getSample(int x, int y, int b, DataBuffer data);

    /** The value of one band, as a `float`. */
    public float getSampleFloat(int x, int y, int b, DataBuffer data) {
        return this.getSample(x, y, b, data);
    }

    /** The value of one band, as a `double`. */
    public double getSampleDouble(int x, int y, int b, DataBuffer data) {
        return this.getSample(x, y, b, data);
    }

    /** Writes the value of one band. */
    public abstract void setSample(int x, int y, int b, int s, DataBuffer data);

    /** Writes the value of one band from a `float`. In an integer model it is truncated. */
    public void setSample(int x, int y, int b, float s, DataBuffer data) {
        this.setSample(x, y, b, (int) s, data);
    }

    /** Writes the value of one band from a `double`. In an integer model it is truncated. */
    public void setSample(int x, int y, int b, double s, DataBuffer data) {
        this.setSample(x, y, b, (int) s, data);
    }

    /** The values of one band over a whole rectangle. */
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

    /** Like the previous one, in `float`. */
    public float[] getSamples(int x, int y, int w, int h, int b, float[] fArray, DataBuffer data) {
        float[] out = fArray == null ? new float[w * h] : fArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                out[k] = this.getSampleFloat(i, j, b, data);
                k = k + 1;
            }
        }
        return out;
    }

    /** Like the previous one, in `double`. */
    public double[] getSamples(int x, int y, int w, int h, int b, double[] dArray,
            DataBuffer data) {
        double[] out = dArray == null ? new double[w * h] : dArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                out[k] = this.getSampleDouble(i, j, b, data);
                k = k + 1;
            }
        }
        return out;
    }

    /** Writes the values of one band over a whole rectangle. */
    public void setSamples(int x, int y, int w, int h, int b, int[] iArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                this.setSample(i, j, b, iArray[k], data);
                k = k + 1;
            }
        }
    }

    /** Like the previous one, from `float`. */
    public void setSamples(int x, int y, int w, int h, int b, float[] fArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                this.setSample(i, j, b, fArray[k], data);
                k = k + 1;
            }
        }
    }

    /** Like the previous one, from `double`. */
    public void setSamples(int x, int y, int w, int h, int b, double[] dArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                this.setSample(i, j, b, dArray[k], data);
                k = k + 1;
            }
        }
    }

    // ---- data elements ----------------------------------------------------------------------

    /**
     * The raw representation of a pixel. See the note of the class.
     *
     * @param obj where to leave it — an array of the type of {@link #getTransferType} — or null
     */
    public abstract Object getDataElements(int x, int y, Object obj, DataBuffer data);

    /**
     * The raw representation of a rectangle.
     *
     * <p>It reserves the array of whichever type applies and delegates pixel by pixel. A subclass
     * that can copy whole blocks overrides it; this is the correct version, not the fast one.
     *
     * @throws IllegalArgumentException if the transfer type is not one of the six
     */
    public Object getDataElements(int x, int y, int w, int h, Object obj, DataBuffer data) {
        int n = this.getNumDataElements();
        int total = w * h * n;
        int type = this.getTransferType();
        Object dest = obj;
        if (type == DataBuffer.TYPE_BYTE) {
            byte[] out = dest == null ? new byte[total] : (byte[]) dest;
            byte[] one = null;
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    one = (byte[]) this.getDataElements(i, j, one, data);
                    System.arraycopy(one, 0, out, k, n);
                    k = k + n;
                }
            }
            return out;
        }
        if (type == DataBuffer.TYPE_USHORT || type == DataBuffer.TYPE_SHORT) {
            short[] out = dest == null ? new short[total] : (short[]) dest;
            short[] one = null;
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    one = (short[]) this.getDataElements(i, j, one, data);
                    System.arraycopy(one, 0, out, k, n);
                    k = k + n;
                }
            }
            return out;
        }
        if (type == DataBuffer.TYPE_INT) {
            int[] out = dest == null ? new int[total] : (int[]) dest;
            int[] one = null;
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    one = (int[]) this.getDataElements(i, j, one, data);
                    System.arraycopy(one, 0, out, k, n);
                    k = k + n;
                }
            }
            return out;
        }
        if (type == DataBuffer.TYPE_FLOAT) {
            float[] out = dest == null ? new float[total] : (float[]) dest;
            float[] one = null;
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    one = (float[]) this.getDataElements(i, j, one, data);
                    System.arraycopy(one, 0, out, k, n);
                    k = k + n;
                }
            }
            return out;
        }
        if (type == DataBuffer.TYPE_DOUBLE) {
            double[] out = dest == null ? new double[total] : (double[]) dest;
            double[] one = null;
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    one = (double[]) this.getDataElements(i, j, one, data);
                    System.arraycopy(one, 0, out, k, n);
                    k = k + n;
                }
            }
            return out;
        }
        throw new IllegalArgumentException("Unsupported transfer type: " + type);
    }

    /** Writes the raw representation of a pixel. */
    public abstract void setDataElements(int x, int y, Object obj, DataBuffer data);

    /**
     * Writes the raw representation of a rectangle.
     *
     * @throws IllegalArgumentException if the transfer type is not one of the six
     */
    public void setDataElements(int x, int y, int w, int h, Object obj, DataBuffer data) {
        int n = this.getNumDataElements();
        int type = this.getTransferType();
        if (type == DataBuffer.TYPE_BYTE) {
            byte[] src = (byte[]) obj;
            byte[] one = new byte[n];
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    System.arraycopy(src, k, one, 0, n);
                    this.setDataElements(i, j, one, data);
                    k = k + n;
                }
            }
            return;
        }
        if (type == DataBuffer.TYPE_USHORT || type == DataBuffer.TYPE_SHORT) {
            short[] src = (short[]) obj;
            short[] one = new short[n];
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    System.arraycopy(src, k, one, 0, n);
                    this.setDataElements(i, j, one, data);
                    k = k + n;
                }
            }
            return;
        }
        if (type == DataBuffer.TYPE_INT) {
            int[] src = (int[]) obj;
            int[] one = new int[n];
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    System.arraycopy(src, k, one, 0, n);
                    this.setDataElements(i, j, one, data);
                    k = k + n;
                }
            }
            return;
        }
        if (type == DataBuffer.TYPE_FLOAT) {
            float[] src = (float[]) obj;
            float[] one = new float[n];
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    System.arraycopy(src, k, one, 0, n);
                    this.setDataElements(i, j, one, data);
                    k = k + n;
                }
            }
            return;
        }
        if (type == DataBuffer.TYPE_DOUBLE) {
            double[] src = (double[]) obj;
            double[] one = new double[n];
            int k = 0;
            for (int j = y; j < y + h; j++) {
                for (int i = x; i < x + w; i++) {
                    System.arraycopy(src, k, one, 0, n);
                    this.setDataElements(i, j, one, data);
                    k = k + n;
                }
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported transfer type: " + type);
    }

    // ---- factories ------------------------------------------------------------------------

    /** A model like this one but of another size. */
    public abstract SampleModel createCompatibleSampleModel(int w, int h);

    /**
     * A model with only some of the bands of this one.
     *
     * <p>It is what makes it possible to look at the red channel of an RGB image **without copying
     * it**: the new model points at the same buffer and only changes what counts as a pixel.
     */
    public abstract SampleModel createSubsetSampleModel(int[] bands);

    /** A buffer of the size and type this model needs. */
    public abstract DataBuffer createDataBuffer();

    /** How many bits each band uses. */
    public abstract int[] getSampleSize();

    /** How many bits that band uses. */
    public abstract int getSampleSize(int band);
}
