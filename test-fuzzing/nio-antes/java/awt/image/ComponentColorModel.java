package java.awt.image;

import java.awt.color.ColorSpace;

/**
 * A colour model with **one band per component**, each one in its own element of the buffer.
 *
 * <p>It is the opposite of {@link DirectColorModel}: there the components are bit fields squeezed
 * into one pixel, here each one is a whole number. In exchange for spending more memory it accepts
 * what the other cannot: colour spaces of any number of components —a CMYK of four, a spectral one
 * of nine—, components of more than eight bits, and floating-point values.
 *
 * <p>Floating point changes what the stored number means. With integer types, the value is a count
 * of steps that has to be divided by its maximum to know what colour it is; with `float` or
 * `double` the value **already is** the component in the units of the {@link ColorSpace}. That is
 * why the "unnormalised" methods make no sense for those types and throw instead of answering:
 * there is no integer scale to return.
 *
 * <p>The other distinction that runs through the class is the sign. The unsigned types cover the
 * whole range of their width; `TYPE_SHORT` is the only signed integer, and for it the methods that
 * ask for a colour in sRGB throw, because half of its values fall outside any colour scale and
 * there is no answer that would not be invented.
 */
public class ComponentColorModel extends ColorModel {

    private final boolean signed;
    private final boolean floating;
    private final float[] minValues;
    private final float[] rangeValues;

    /**
     * The general constructor.
     *
     * @throws IllegalArgumentException if `bits` is not enough for every component, if some width
     *     does not fit in the transfer type, or if the type is not one of the six
     * @throws NullPointerException if the colour space is missing
     */
    public ComponentColorModel(ColorSpace colorSpace, int[] bits, boolean hasAlpha,
            boolean isAlphaPremultiplied, int transparency, int transferType) {
        super(bitsPerPixel(bits, colorSpace, hasAlpha, transferType),
                createBitsArray(bits, colorSpace, hasAlpha, transferType), colorSpace, hasAlpha,
                isAlphaPremultiplied, transparency, transferType);
        if (transferType == DataBuffer.TYPE_BYTE || transferType == DataBuffer.TYPE_USHORT
                || transferType == DataBuffer.TYPE_INT) {
            this.signed = false;
            this.floating = false;
        } else if (transferType == DataBuffer.TYPE_SHORT) {
            this.signed = true;
            this.floating = false;
        } else if (transferType == DataBuffer.TYPE_FLOAT
                || transferType == DataBuffer.TYPE_DOUBLE) {
            this.signed = true;
            this.floating = true;
        } else {
            throw new IllegalArgumentException(
                    "This constructor does not support transferType " + transferType);
        }
        int tam = DataBuffer.getDataTypeSize(transferType);
        for (int i = 0; i < this.numComponents; i++) {
            if (this.nBits[i] > tam) {
                throw new IllegalArgumentException("Number of bits for component " + i
                        + " is greater than the size of the transfer type " + transferType);
            }
        }
        // The range of each component is stored because every conversion needs it and asking the
        // colour space for it on every pixel would be one virtual call per component and per pixel.
        this.minValues = new float[this.numComponents];
        this.rangeValues = new float[this.numComponents];
        for (int i = 0; i < this.numColorComponents; i++) {
            this.minValues[i] = colorSpace.getMinValue(i);
            this.rangeValues[i] = colorSpace.getMaxValue(i) - this.minValues[i];
        }
        if (this.supportsAlpha) {
            this.minValues[this.numColorComponents] = 0.0f;
            this.rangeValues[this.numColorComponents] = 1.0f;
        }
    }

    /**
     * Like the previous one, with every component of the width of the transfer type.
     *
     * @throws IllegalArgumentException if the type is not one of the six
     * @throws NullPointerException if the colour space is missing
     */
    public ComponentColorModel(ColorSpace colorSpace, boolean hasAlpha,
            boolean isAlphaPremultiplied, int transparency, int transferType) {
        this(colorSpace, null, hasAlpha, isAlphaPremultiplied, transparency, transferType);
    }

    /**
     * The component widths, or those of the type if none were given.
     *
     * @throws IllegalArgumentException if the array does not carry every component
     */
    private static int[] createBitsArray(int[] origBits, ColorSpace colorSpace, boolean hasAlpha,
            int transferType) {
        int numComponents = colorSpace.getNumComponents() + (hasAlpha ? 1 : 0);
        if (origBits == null) {
            int[] bits = new int[numComponents];
            int bitsPer = DataBuffer.getDataTypeSize(transferType);
            for (int i = 0; i < numComponents; i++) {
                bits[i] = bitsPer;
            }
            return bits;
        }
        if (origBits.length < numComponents) {
            throw new IllegalArgumentException("Number of color/alpha components should be "
                    + numComponents + " but length of bits array is " + origBits.length);
        }
        return origBits;
    }

    /** The bits per pixel: the sum of the widths of every component. */
    private static int bitsPerPixel(int[] origBits, ColorSpace colorSpace, boolean hasAlpha,
            int transferType) {
        int[] bits = createBitsArray(origBits, colorSpace, hasAlpha, transferType);
        int numComponents = colorSpace.getNumComponents() + (hasAlpha ? 1 : 0);
        int total = 0;
        for (int i = 0; i < numComponents; i++) {
            total = total + bits[i];
        }
        return total;
    }

    /** The greatest value that integer component can take. */
    private int maxValueOf(int idx) {
        return (1 << this.nBits[idx]) - 1;
    }

    /**
     * Reads the raw components of a pixel, each one as an uninterpreted `float`.
     *
     * @throws UnsupportedOperationException if the type is not one of the six
     * @throws ClassCastException if the array is not of the transfer type
     */
    private float[] readRaw(Object inData) {
        float[] out = new float[this.numComponents];
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            byte[] d = (byte[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = d[i] & 0xFF;
            }
        } else if (this.transferType == DataBuffer.TYPE_USHORT) {
            short[] d = (short[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = d[i] & 0xFFFF;
            }
        } else if (this.transferType == DataBuffer.TYPE_SHORT) {
            short[] d = (short[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = d[i];
            }
        } else if (this.transferType == DataBuffer.TYPE_INT) {
            int[] d = (int[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = d[i];
            }
        } else if (this.transferType == DataBuffer.TYPE_FLOAT) {
            float[] d = (float[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = d[i];
            }
        } else if (this.transferType == DataBuffer.TYPE_DOUBLE) {
            double[] d = (double[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = (float) d[i];
            }
        } else {
            throw new UnsupportedOperationException(
                    "This method has not been implemented for transferType " + this.transferType);
        }
        return out;
    }

    /**
     * Stores raw components in an array of the transfer type.
     *
     * @throws UnsupportedOperationException if the type is not one of the six
     */
    private Object writeRaw(float[] raw, Object obj) {
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            byte[] out = obj == null ? new byte[this.numComponents] : (byte[]) obj;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = (byte) raw[i];
            }
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_USHORT
                || this.transferType == DataBuffer.TYPE_SHORT) {
            short[] out = obj == null ? new short[this.numComponents] : (short[]) obj;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = (short) raw[i];
            }
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_INT) {
            int[] out = obj == null ? new int[this.numComponents] : (int[]) obj;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = (int) raw[i];
            }
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_FLOAT) {
            float[] out = obj == null ? new float[this.numComponents] : (float[]) obj;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = raw[i];
            }
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_DOUBLE) {
            double[] out = obj == null ? new double[this.numComponents] : (double[]) obj;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = raw[i];
            }
            return out;
        }
        throw new UnsupportedOperationException(
                "This method has not been implemented for transferType " + this.transferType);
    }

    /**
     * Raw components brought into the units of the colour space, with the premultiplication undone.
     *
     * <p>In floating point the stored value is in those units already; in integers one has to
     * divide by the maximum of each component and stretch to the range of the space.
     */
    private float[] toSpace(float[] raw) {
        float[] out = new float[this.numComponents];
        float alpha = 1.0f;
        if (this.supportsAlpha) {
            if (this.floating) {
                alpha = raw[this.numColorComponents];
            } else {
                alpha = raw[this.numColorComponents] / this.maxValueOf(this.numColorComponents);
            }
            out[this.numColorComponents] = alpha;
        }
        for (int i = 0; i < this.numColorComponents; i++) {
            float v;
            if (this.floating) {
                v = raw[i];
            } else {
                v = this.minValues[i] + (raw[i] / this.maxValueOf(i)) * this.rangeValues[i];
            }
            if (this.supportsAlpha && this.isAlphaPremultiplied) {
                if (alpha == 0.0f) {
                    v = this.minValues[i];
                } else {
                    v = this.minValues[i] + (v - this.minValues[i]) / alpha;
                }
            }
            out[i] = v;
        }
        return out;
    }

    /** The inverse of {@link #toSpace}. */
    private float[] fromSpace(float[] comps) {
        float[] out = new float[this.numComponents];
        float alpha = 1.0f;
        if (this.supportsAlpha) {
            alpha = comps[this.numColorComponents];
        }
        for (int i = 0; i < this.numColorComponents; i++) {
            float v = comps[i];
            if (this.supportsAlpha && this.isAlphaPremultiplied) {
                v = this.minValues[i] + (v - this.minValues[i]) * alpha;
            }
            if (this.floating) {
                out[i] = v;
            } else {
                float norm = (v - this.minValues[i]) / this.rangeValues[i];
                out[i] = (int) (norm * this.maxValueOf(i) + 0.5f);
            }
        }
        if (this.supportsAlpha) {
            if (this.floating) {
                out[this.numColorComponents] = alpha;
            } else {
                out[this.numColorComponents] =
                        (int) (alpha * this.maxValueOf(this.numColorComponents) + 0.5f);
            }
        }
        return out;
    }

    /**
     * The three sRGB components, from 0 to 255, of a pixel already brought into the colour space.
     */
    private int[] toSrgb(float[] comps) {
        float[] color = new float[this.numColorComponents];
        for (int i = 0; i < this.numColorComponents; i++) {
            color[i] = comps[i];
        }
        float[] rgb = this.colorSpace.toRGB(color);
        int[] out = new int[3];
        for (int i = 0; i < 3; i++) {
            float v = rgb[i];
            if (v < 0.0f) {
                v = 0.0f;
            }
            if (v > 1.0f) {
                v = 1.0f;
            }
            out[i] = (int) (v * 255.0f + 0.5f);
        }
        return out;
    }

    /**
     * Checks that an `int` is enough to represent the pixel.
     *
     * @throws IllegalArgumentException if the pixel has more than one component or if it is signed
     */
    private void requireOneComponent() {
        if (this.numComponents > 1) {
            throw new IllegalArgumentException("More than one component per pixel");
        }
        if (this.signed) {
            throw new IllegalArgumentException("Component value is signed");
        }
    }

    /**
     * The red of the pixel, from 0 to 255 and in sRGB.
     *
     * @throws IllegalArgumentException if the pixel has more than one component or is signed
     */
    public int getRed(int pixel) {
        this.requireOneComponent();
        float[] raw = new float[1];
        raw[0] = pixel;
        return this.toSrgb(this.toSpace(raw))[0];
    }

    /**
     * The green of the pixel, from 0 to 255 and in sRGB.
     *
     * @throws IllegalArgumentException if the pixel has more than one component or is signed
     */
    public int getGreen(int pixel) {
        this.requireOneComponent();
        float[] raw = new float[1];
        raw[0] = pixel;
        return this.toSrgb(this.toSpace(raw))[1];
    }

    /**
     * The blue of the pixel, from 0 to 255 and in sRGB.
     *
     * @throws IllegalArgumentException if the pixel has more than one component or is signed
     */
    public int getBlue(int pixel) {
        this.requireOneComponent();
        float[] raw = new float[1];
        raw[0] = pixel;
        return this.toSrgb(this.toSpace(raw))[2];
    }

    /**
     * The alpha of the pixel.
     *
     * <p>A model of a single component has no alpha, so the answer is 255.
     *
     * @throws IllegalArgumentException if the pixel has more than one component or is signed
     */
    public int getAlpha(int pixel) {
        this.requireOneComponent();
        return 255;
    }

    /**
     * The whole pixel as ARGB.
     *
     * @throws IllegalArgumentException if the pixel has more than one component or is signed
     */
    public int getRGB(int pixel) {
        this.requireOneComponent();
        int[] rgb = this.toSrgb(this.toSpace(new float[] { pixel }));
        return 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    /** The red of a raw pixel, from 0 to 255 and in sRGB. */
    public int getRed(Object inData) {
        return this.toSrgb(this.toSpace(this.readRaw(inData)))[0];
    }

    /** The green of a raw pixel, from 0 to 255 and in sRGB. */
    public int getGreen(Object inData) {
        return this.toSrgb(this.toSpace(this.readRaw(inData)))[1];
    }

    /** The blue of a raw pixel, from 0 to 255 and in sRGB. */
    public int getBlue(Object inData) {
        return this.toSrgb(this.toSpace(this.readRaw(inData)))[2];
    }

    /** The alpha of a raw pixel, from 0 to 255; 255 if the model has no alpha. */
    public int getAlpha(Object inData) {
        if (!this.supportsAlpha) {
            return 255;
        }
        float[] comps = this.toSpace(this.readRaw(inData));
        float a = comps[this.numColorComponents];
        if (a < 0.0f) {
            a = 0.0f;
        }
        if (a > 1.0f) {
            a = 1.0f;
        }
        return (int) (a * 255.0f + 0.5f);
    }

    /** A raw pixel as ARGB with eight bits per channel. */
    public int getRGB(Object inData) {
        float[] comps = this.toSpace(this.readRaw(inData));
        int[] rgb = this.toSrgb(comps);
        int a = 255;
        if (this.supportsAlpha) {
            float av = comps[this.numColorComponents];
            if (av < 0.0f) {
                av = 0.0f;
            }
            if (av > 1.0f) {
                av = 1.0f;
            }
            a = (int) (av * 255.0f + 0.5f);
        }
        return (a << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    /** An ARGB brought into a raw pixel of this model. */
    public Object getDataElements(int rgb, Object pixel) {
        float[] rgbf = new float[3];
        rgbf[0] = ((rgb >> 16) & 0xFF) / 255.0f;
        rgbf[1] = ((rgb >> 8) & 0xFF) / 255.0f;
        rgbf[2] = (rgb & 0xFF) / 255.0f;
        float[] color = this.colorSpace.fromRGB(rgbf);
        float[] comps = new float[this.numComponents];
        for (int i = 0; i < this.numColorComponents; i++) {
            comps[i] = color[i];
        }
        if (this.supportsAlpha) {
            comps[this.numColorComponents] = (rgb >>> 24) / 255.0f;
        }
        return this.writeRaw(this.fromSpace(comps), pixel);
    }

    /**
     * The raw components of a pixel in an `int`.
     *
     * @throws IllegalArgumentException if the pixel has more than one component
     */
    public int[] getComponents(int pixel, int[] components, int offset) {
        if (this.numComponents > 1) {
            throw new IllegalArgumentException("More than one component per pixel");
        }
        int[] out = components;
        if (out == null) {
            out = new int[offset + 1];
        }
        out[offset] = pixel;
        return out;
    }

    /**
     * The raw components of a raw pixel.
     *
     * @throws IllegalArgumentException if the type is floating point, which has no integer form
     */
    public int[] getComponents(Object pixel, int[] components, int offset) {
        if (this.floating) {
            throw new IllegalArgumentException(
                    "This ColorModel does not support the unnormalized form");
        }
        float[] raw = this.readRaw(pixel);
        int[] out = components;
        if (out == null) {
            out = new int[offset + this.numComponents];
        }
        for (int i = 0; i < this.numComponents; i++) {
            out[offset + i] = (int) raw[i];
        }
        return out;
    }

    /**
     * Normalised components brought into the scale of the image.
     *
     * @throws IllegalArgumentException if the type is floating point, or if the array does not
     *     carry every component
     */
    public int[] getUnnormalizedComponents(float[] normComponents, int normOffset,
            int[] components, int offset) {
        if (this.floating) {
            throw new IllegalArgumentException(
                    "This ColorModel does not support the unnormalized form");
        }
        return super.getUnnormalizedComponents(normComponents, normOffset, components, offset);
    }

    /**
     * Components of the image brought into 0..1.
     *
     * @throws IllegalArgumentException if the type is floating point, or if the array does not
     *     carry every component
     */
    public float[] getNormalizedComponents(int[] components, int offset, float[] normComponents,
            int normOffset) {
        if (this.floating) {
            throw new IllegalArgumentException(
                    "This ColorModel does not support the unnormalized form");
        }
        return super.getNormalizedComponents(components, offset, normComponents, normOffset);
    }

    /**
     * Raw components brought into a pixel in an `int`.
     *
     * @throws IllegalArgumentException if the pixel has more than one component
     */
    public int getDataElement(int[] components, int offset) {
        if (this.numComponents > 1) {
            throw new IllegalArgumentException("More than one component per pixel");
        }
        if (this.floating) {
            throw new IllegalArgumentException(
                    "This ColorModel does not support the unnormalized form");
        }
        return components[offset];
    }

    /**
     * Raw components brought into a raw pixel.
     *
     * @throws IllegalArgumentException if the type is floating point
     */
    public Object getDataElements(int[] components, int offset, Object obj) {
        if (this.floating) {
            throw new IllegalArgumentException(
                    "This ColorModel does not support the unnormalized form");
        }
        float[] raw = new float[this.numComponents];
        for (int i = 0; i < this.numComponents; i++) {
            raw[i] = components[offset + i];
        }
        return this.writeRaw(raw, obj);
    }

    /**
     * Normalised components brought into a pixel in an `int`.
     *
     * @throws IllegalArgumentException if the pixel has more than one component
     */
    public int getDataElement(float[] normComponents, int normOffset) {
        if (this.numComponents > 1) {
            throw new IllegalArgumentException("More than one component per pixel");
        }
        float[] comps = new float[this.numComponents];
        for (int i = 0; i < this.numComponents; i++) {
            comps[i] = this.minValues[i]
                    + normComponents[normOffset + i] * this.rangeValues[i];
        }
        return (int) this.fromSpace(comps)[0];
    }

    /** Normalised components brought into a raw pixel. */
    public Object getDataElements(float[] normComponents, int normOffset, Object obj) {
        float[] comps = new float[this.numComponents];
        for (int i = 0; i < this.numComponents; i++) {
            comps[i] = this.minValues[i]
                    + normComponents[normOffset + i] * this.rangeValues[i];
        }
        return this.writeRaw(this.fromSpace(comps), obj);
    }

    /**
     * The normalised components of a raw pixel.
     *
     * <p>"Normalised" here means from 0 to 1, not in the units of the colour space: that is why the
     * result is brought back to the unit range even if the space has another one.
     */
    public float[] getNormalizedComponents(Object pixel, float[] normComponents, int normOffset) {
        float[] comps = this.toSpace(this.readRaw(pixel));
        float[] out = normComponents;
        if (out == null) {
            out = new float[this.numComponents + normOffset];
        }
        for (int i = 0; i < this.numComponents; i++) {
            out[normOffset + i] = (comps[i] - this.minValues[i]) / this.rangeValues[i];
        }
        return out;
    }

    /** An interleaved sample model with one band per component. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        int[] bandOffsets = new int[this.numComponents];
        for (int i = 0; i < this.numComponents; i++) {
            bandOffsets[i] = i;
        }
        if (this.transferType == DataBuffer.TYPE_BYTE
                || this.transferType == DataBuffer.TYPE_USHORT) {
            return new PixelInterleavedSampleModel(this.transferType, w, h, this.numComponents,
                    w * this.numComponents, bandOffsets);
        }
        return new ComponentSampleModel(this.transferType, w, h, this.numComponents,
                w * this.numComponents, bandOffsets);
    }

    /** Whether that sample model has one band per component and the same type. */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        if (!(sm instanceof ComponentSampleModel)) {
            return false;
        }
        if (this.numComponents != sm.getNumBands()) {
            return false;
        }
        return sm.getTransferType() == this.transferType;
    }

    /**
     * A raster with one band per component.
     *
     * <p>With `byte` and fewer than eight bits per pixel what comes out is a packed raster of
     * several pixels per element, which is what corresponds to a 1-, 2- or 4-bit image.
     *
     * @throws IllegalArgumentException if the size is empty
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") cannot be <= 0");
        }
        if (this.transferType == DataBuffer.TYPE_BYTE
                && (this.pixel_bits == 1 || this.pixel_bits == 2 || this.pixel_bits == 4)) {
            return Raster.createPackedRaster(new DataBufferByte(w * h), w, h, this.pixel_bits,
                    null);
        }
        SampleModel sm = this.createCompatibleSampleModel(w, h);
        return Raster.createWritableRaster(sm, sm.createDataBuffer(), null);
    }

    /** Whether that raster has one band per component, with enough room in each one. */
    public boolean isCompatibleRaster(Raster raster) {
        SampleModel sm = raster.getSampleModel();
        if (!(sm instanceof ComponentSampleModel)) {
            return false;
        }
        if (sm.getNumBands() != this.numComponents) {
            return false;
        }
        for (int i = 0; i < this.nBits.length; i++) {
            if (sm.getSampleSize(i) < this.nBits[i]) {
                return false;
            }
        }
        return raster.getTransferType() == this.transferType;
    }

    /**
     * The alpha channel as a one-band raster **over the same data**.
     *
     * <p>It returns `null` if the model has no alpha.
     */
    public WritableRaster getAlphaRaster(WritableRaster raster) {
        if (!this.hasAlpha()) {
            return null;
        }
        int x = raster.getMinX();
        int y = raster.getMinY();
        int[] band = new int[1];
        band[0] = raster.getNumBands() - 1;
        return raster.createWritableChild(x, y, raster.getWidth(), raster.getHeight(), x, y, band);
    }

    /**
     * Premultiplies the raster by its alpha, or undoes it, **in place**.
     *
     * <p>It returns the model that describes the raster after the change; if it was as asked for
     * already, or if there is no alpha, it returns itself without touching anything.
     */
    public ColorModel coerceData(WritableRaster raster, boolean isAlphaPremultiplied) {
        if (!this.supportsAlpha || this.isAlphaPremultiplied == isAlphaPremultiplied) {
            return this;
        }
        int w = raster.getWidth();
        int h = raster.getHeight();
        int aIdx = this.numColorComponents;
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        float[] pixel = null;
        for (int y = minY; y < minY + h; y++) {
            for (int x = minX; x < minX + w; x++) {
                pixel = raster.getPixel(x, y, pixel);
                float normAlpha;
                if (this.floating) {
                    normAlpha = pixel[aIdx];
                } else {
                    normAlpha = pixel[aIdx] / this.maxValueOf(aIdx);
                }
                if (isAlphaPremultiplied) {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = pixel[c] * normAlpha;
                    }
                } else if (normAlpha != 0.0f) {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = pixel[c] / normAlpha;
                    }
                } else {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = 0.0f;
                    }
                }
                raster.setPixel(x, y, pixel);
            }
        }
        return new ComponentColorModel(this.colorSpace, this.nBits, this.supportsAlpha,
                isAlphaPremultiplied, this.transparency, this.transferType);
    }

    public String toString() {
        return "ComponentColorModel: #pixelBits = " + this.pixel_bits + " numComponents = "
                + this.numComponents + " color space = " + this.colorSpace + " transparency = "
                + this.transparency + " has alpha = " + this.supportsAlpha + " isAlphaPre = "
                + this.isAlphaPremultiplied;
    }
}
