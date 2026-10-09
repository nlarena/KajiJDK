package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;

/**
 * What **colour** a pixel is.
 *
 * <p>It is the third and last piece of an image, and the only one that talks about colour. The
 * {@link DataBuffer} has numbers; the {@link SampleModel} knows which of those numbers make up a
 * pixel; the colour model knows what colour it is. Without it, `(255, 0, 0)` is not red: it is
 * three numbers.
 *
 * <p>The translation goes back and forth between three representations of the same pixel, and
 * almost the whole class is going from one to another:
 *
 * <ul>
 *   <li><strong>the pixel</strong> — an `int`, or an array if it does not fit in one, just as it is
 *       stored;
 *   <li><strong>the components</strong> — one integer value per band, separated already but still
 *       in the scale of the image, which may have bands of different bit counts;
 *   <li><strong>the normalised components</strong> — one `float` per band in the range of the
 *       {@link ColorSpace}, which is where the colour really exists and where two images of
 *       different formats can be compared.
 * </ul>
 *
 * <p>The subclasses override the conversions their format does fast; what they do not override
 * falls into the general methods here, which always go through the normalised components. The ones
 * that do **not** have an honest general version —the ones that depend on how the pixel is
 * assembled, such as {@link #getDataElements(int, Object)} or {@link #createCompatibleSampleModel}—
 * throw `UnsupportedOperationException` instead of inventing an answer.
 *
 * <p>The premultiplied alpha deserves a note, because it is not a convention but a sum that has
 * been done already. With `isAlphaPremultiplied`, the stored colour components **are already
 * multiplied** by the alpha: a red at half transparency is stored as 127 and not as 255. That makes
 * compositing trivial —adding is superimposing— and makes recovering the original colour have to
 * divide, with the division by zero when the alpha is zero. That branch shows up in every
 * conversion of this class.
 */
public abstract class ColorModel implements Transparency {

    /** How many bits a pixel takes. */
    protected int pixel_bits;

    /** The type a raw pixel is transferred with. */
    protected int transferType;

    /** How many bits each component uses, or `null` if they were not declared. */
    int[] nBits;

    /** The greatest of `nBits`. */
    int maxBits;

    // These fields are package-private and not private, just as in the JDK: the subclasses in here
    // adjust them. PackedColorModel, for instance, lowers the transparency to BITMASK when it finds
    // out that the alpha has a single bit, and that is only known after taking the masks apart.
    ColorSpace colorSpace;
    int colorSpaceType;
    int numComponents;
    int numColorComponents;
    boolean supportsAlpha;
    boolean isAlphaPremultiplied;
    int transparency;
    boolean isSrgb;

    private static ColorModel rgbDefault;

    /**
     * The usual ARGB model: 32 bits, translucent alpha and sRGB.
     *
     * <p>It is the format colour is spoken of in throughout the API when nothing else is said: the
     * `int` that {@link #getRGB(int)} returns and the one {@link #getDataElements(int, Object)}
     * takes are in this model, no matter how the image is stored.
     */
    public static ColorModel getRGBdefault() {
        synchronized (ColorModel.class) {
            if (rgbDefault == null) {
                rgbDefault = new DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF,
                        0xFF000000);
            }
            return rgbDefault;
        }
    }

    /** The smallest type a pixel of that many bits fits in. */
    static int getDefaultTransferType(int pixelBits) {
        if (pixelBits <= 8) {
            return DataBuffer.TYPE_BYTE;
        }
        if (pixelBits <= 16) {
            return DataBuffer.TYPE_USHORT;
        }
        if (pixelBits <= 32) {
            return DataBuffer.TYPE_INT;
        }
        return DataBuffer.TYPE_UNDEFINED;
    }

    /**
     * A translucent ARGB model in sRGB, of that many bits per pixel.
     *
     * <p>It is the convenient constructor, and the one that leaves the model half declared: it does
     * not say how many bits each component uses, so the conversions that need that count —the
     * normalised ones— throw instead of answering.
     *
     * @throws IllegalArgumentException if `bits` is not positive
     */
    public ColorModel(int bits) {
        if (bits < 1) {
            throw new IllegalArgumentException("Number of bits must be > 0");
        }
        this.pixel_bits = bits;
        this.colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        this.colorSpaceType = ColorSpace.TYPE_RGB;
        this.numComponents = 4;
        this.numColorComponents = 3;
        this.supportsAlpha = true;
        this.isAlphaPremultiplied = false;
        this.transparency = Transparency.TRANSLUCENT;
        this.isSrgb = true;
        this.nBits = null;
        this.maxBits = bits;
        this.transferType = getDefaultTransferType(bits);
    }

    /**
     * The general constructor.
     *
     * <p>Without alpha, `isAlphaPremultiplied` and `transparency` are ignored and stay at `false`
     * and `OPAQUE`: with no alpha channel there is nothing to premultiply and no transparency to
     * declare, and leaving them as they were passed would be storing a contradiction.
     *
     * @throws IllegalArgumentException if `bits` is not enough for every component, if the
     *     transparency is not one of the three, if some width is negative, if they are all zero, or
     *     if the bits per pixel are not positive
     * @throws NullPointerException if the colour space is missing
     */
    protected ColorModel(int pixel_bits, int[] bits, ColorSpace cspace, boolean hasAlpha,
            boolean isAlphaPremultiplied, int transparency, int transferType) {
        this.colorSpace = cspace;
        this.colorSpaceType = cspace.getType();
        this.numColorComponents = cspace.getNumComponents();
        this.numComponents = this.numColorComponents + (hasAlpha ? 1 : 0);
        this.supportsAlpha = hasAlpha;
        if (bits.length < this.numComponents) {
            throw new IllegalArgumentException("Number of color/alpha components should be "
                    + this.numComponents + " but length of bits array is " + bits.length);
        }
        if (transparency < Transparency.OPAQUE || transparency > Transparency.TRANSLUCENT) {
            throw new IllegalArgumentException("Unknown transparency: " + transparency);
        }
        if (!this.supportsAlpha) {
            this.isAlphaPremultiplied = false;
            this.transparency = Transparency.OPAQUE;
        } else {
            this.isAlphaPremultiplied = isAlphaPremultiplied;
            this.transparency = transparency;
        }
        this.nBits = bits.clone();
        this.pixel_bits = pixel_bits;
        if (pixel_bits <= 0) {
            throw new IllegalArgumentException("Number of pixel bits must be > 0");
        }
        this.maxBits = 0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] < 0) {
                throw new IllegalArgumentException("Number of bits must be >= 0");
            }
            if (this.maxBits < bits[i]) {
                this.maxBits = bits[i];
            }
        }
        if (this.maxBits == 0) {
            throw new IllegalArgumentException(
                    "There must be at least one component with > 0 pixel bits.");
        }
        this.isSrgb = cspace == ColorSpace.getInstance(ColorSpace.CS_sRGB);
        this.transferType = transferType;
    }

    /** Whether it has an alpha channel. */
    public final boolean hasAlpha() {
        return this.supportsAlpha;
    }

    /** Whether the colour components come multiplied by the alpha already. */
    public final boolean isAlphaPremultiplied() {
        return this.isAlphaPremultiplied;
    }

    /** The type a raw pixel is transferred with. */
    public final int getTransferType() {
        return this.transferType;
    }

    /** How many bits a pixel takes. */
    public int getPixelSize() {
        return this.pixel_bits;
    }

    /**
     * How many bits that component uses.
     *
     * @throws NullPointerException if the model did not declare the widths
     * @throws ArrayIndexOutOfBoundsException if the component does not exist
     */
    public int getComponentSize(int componentIdx) {
        if (this.nBits == null) {
            throw new NullPointerException("Number of bits array is null.");
        }
        return this.nBits[componentIdx];
    }

    /** How many bits each component uses, or `null` if they were not declared. */
    public int[] getComponentSize() {
        if (this.nBits == null) {
            return null;
        }
        return this.nBits.clone();
    }

    /** `OPAQUE`, `BITMASK` or `TRANSLUCENT`. */
    public int getTransparency() {
        return this.transparency;
    }

    /** How many components a pixel has, counting the alpha. */
    public int getNumComponents() {
        return this.numComponents;
    }

    /** How many components a pixel has without counting the alpha. */
    public int getNumColorComponents() {
        return this.numColorComponents;
    }

    /** The colour space. */
    public final ColorSpace getColorSpace() {
        return this.colorSpace;
    }

    /** Whether the colour space is the default sRGB. */
    final boolean isSrgb() {
        return this.isSrgb;
    }

    /** The type of the colour space, without having to ask it for it. */
    final int getColorSpaceType() {
        return this.colorSpaceType;
    }

    /** The red of the pixel, from 0 to 255 and in sRGB. */
    public abstract int getRed(int pixel);

    /** The green of the pixel, from 0 to 255 and in sRGB. */
    public abstract int getGreen(int pixel);

    /** The blue of the pixel, from 0 to 255 and in sRGB. */
    public abstract int getBlue(int pixel);

    /** The alpha of the pixel, from 0 to 255. */
    public abstract int getAlpha(int pixel);

    /** The whole pixel as ARGB with eight bits per channel. */
    public int getRGB(int pixel) {
        return (this.getAlpha(pixel) << 24) | (this.getRed(pixel) << 16)
                | (this.getGreen(pixel) << 8) | this.getBlue(pixel);
    }

    /**
     * A raw pixel brought into an `int`.
     *
     * <p>It only works if the pixel fits in one element; if it is several, this class does not know
     * how to join them and it has to be overridden.
     *
     * @throws UnsupportedOperationException if the type does not fit in an `int` or the pixel takes
     *     more than one element
     * @throws ClassCastException if the array is not of the transfer type
     */
    private int unPixel(Object inData) {
        int pixel;
        int length;
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            byte[] bdata = (byte[]) inData;
            pixel = bdata[0] & 0xFF;
            length = bdata.length;
        } else if (this.transferType == DataBuffer.TYPE_USHORT) {
            short[] sdata = (short[]) inData;
            pixel = sdata[0] & 0xFFFF;
            length = sdata.length;
        } else if (this.transferType == DataBuffer.TYPE_INT) {
            int[] idata = (int[]) inData;
            pixel = idata[0];
            length = idata.length;
        } else {
            throw new UnsupportedOperationException(
                    "This method has not been implemented for transferType " + this.transferType);
        }
        if (length != 1) {
            throw new UnsupportedOperationException(
                    "This method is not supported by this color model");
        }
        return pixel;
    }

    /**
     * The red of a raw pixel.
     *
     * @throws UnsupportedOperationException if the pixel does not fit in an `int`
     */
    public int getRed(Object inData) {
        return this.getRed(this.unPixel(inData));
    }

    /**
     * The green of a raw pixel.
     *
     * @throws UnsupportedOperationException if the pixel does not fit in an `int`
     */
    public int getGreen(Object inData) {
        return this.getGreen(this.unPixel(inData));
    }

    /**
     * The blue of a raw pixel.
     *
     * @throws UnsupportedOperationException if the pixel does not fit in an `int`
     */
    public int getBlue(Object inData) {
        return this.getBlue(this.unPixel(inData));
    }

    /**
     * The alpha of a raw pixel.
     *
     * @throws UnsupportedOperationException if the pixel does not fit in an `int`
     */
    public int getAlpha(Object inData) {
        return this.getAlpha(this.unPixel(inData));
    }

    /**
     * A raw pixel as ARGB with eight bits per channel.
     *
     * @throws UnsupportedOperationException if the pixel does not fit in an `int`
     */
    public int getRGB(Object inData) {
        return (this.getAlpha(inData) << 24) | (this.getRed(inData) << 16)
                | (this.getGreen(inData) << 8) | this.getBlue(inData);
    }

    /**
     * An ARGB brought into a pixel of this model.
     *
     * <p>There is no general version: assembling the pixel depends entirely on how the subclass
     * stores it.
     *
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public Object getDataElements(int rgb, Object pixel) {
        throw new UnsupportedOperationException("This method is not supported by this color model.");
    }

    /**
     * The components of a pixel.
     *
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public int[] getComponents(int pixel, int[] components, int offset) {
        throw new UnsupportedOperationException("This method is not supported by this color model.");
    }

    /**
     * The components of a raw pixel.
     *
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public int[] getComponents(Object pixel, int[] components, int offset) {
        throw new UnsupportedOperationException("This method is not supported by this color model.");
    }

    /**
     * Normalised components brought into the scale of the image.
     *
     * <p>With premultiplied alpha each colour component is multiplied by the alpha before being
     * scaled, which is exactly what premultiplying means.
     *
     * @throws UnsupportedOperationException if the model did not declare the component widths
     * @throws IllegalArgumentException if the input array does not carry every component
     */
    public int[] getUnnormalizedComponents(float[] normComponents, int normOffset,
            int[] components, int offset) {
        if (this.colorSpace == null) {
            throw new UnsupportedOperationException(
                    "This method is not supported by this color model.");
        }
        if (this.nBits == null) {
            throw new UnsupportedOperationException("This method is not supported.  "
                    + "Unable to determine #bits per component.");
        }
        if (normComponents.length - normOffset < this.numComponents) {
            throw new IllegalArgumentException(
                    "Incorrect number of components.  Expecting " + this.numComponents);
        }
        int[] out = components;
        if (out == null) {
            out = new int[offset + this.numComponents];
        }
        if (this.supportsAlpha && this.isAlphaPremultiplied) {
            float normAlpha = normComponents[normOffset + this.numColorComponents];
            for (int i = 0; i < this.numColorComponents; i++) {
                out[offset + i] = (int) (normComponents[normOffset + i]
                        * ((1 << this.nBits[i]) - 1) * normAlpha + 0.5f);
            }
            out[offset + this.numColorComponents] = (int) (normAlpha
                    * ((1 << this.nBits[this.numColorComponents]) - 1) + 0.5f);
        } else {
            for (int i = 0; i < this.numComponents; i++) {
                out[offset + i] = (int) (normComponents[normOffset + i]
                        * ((1 << this.nBits[i]) - 1) + 0.5f);
            }
        }
        return out;
    }

    /**
     * Components of the image brought into the scale of the colour space.
     *
     * <p>It is the inverse of {@link #getUnnormalizedComponents}. With premultiplied alpha one has
     * to **divide** by the alpha to recover the colour, and with alpha zero there is no colour to
     * recover: the pixel is invisible and its components come out at zero, which is all that can be
     * said.
     *
     * @throws UnsupportedOperationException if the model did not declare the component widths
     * @throws IllegalArgumentException if the input array does not carry every component
     */
    public float[] getNormalizedComponents(int[] components, int offset, float[] normComponents,
            int normOffset) {
        if (this.colorSpace == null) {
            throw new UnsupportedOperationException(
                    "This method is not supported by this color model.");
        }
        if (this.nBits == null) {
            throw new UnsupportedOperationException("This method is not supported.  "
                    + "Unable to determine #bits per component.");
        }
        if (components.length - offset < this.numComponents) {
            throw new IllegalArgumentException(
                    "Incorrect number of components.  Expecting " + this.numComponents);
        }
        float[] out = normComponents;
        if (out == null) {
            out = new float[this.numComponents + normOffset];
        }
        if (this.supportsAlpha && this.isAlphaPremultiplied) {
            float normAlpha = (float) components[offset + this.numColorComponents];
            normAlpha = normAlpha / (float) ((1 << this.nBits[this.numColorComponents]) - 1);
            if (normAlpha != 0.0f) {
                for (int i = 0; i < this.numColorComponents; i++) {
                    out[normOffset + i] = ((float) components[offset + i])
                            / (normAlpha * ((float) ((1 << this.nBits[i]) - 1)));
                }
            } else {
                for (int i = 0; i < this.numColorComponents; i++) {
                    out[normOffset + i] = 0.0f;
                }
            }
            out[normOffset + this.numColorComponents] = normAlpha;
        } else {
            for (int i = 0; i < this.numComponents; i++) {
                out[normOffset + i] = ((float) components[offset + i])
                        / ((float) ((1 << this.nBits[i]) - 1));
            }
        }
        return out;
    }

    /**
     * Components brought into a pixel in an `int`.
     *
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public int getDataElement(int[] components, int offset) {
        throw new UnsupportedOperationException("This method is not supported by this color model.");
    }

    /**
     * Components brought into a raw pixel.
     *
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public Object getDataElements(int[] components, int offset, Object obj) {
        throw new UnsupportedOperationException("This method is not supported by this color model.");
    }

    /**
     * Normalised components brought into a pixel in an `int`.
     *
     * <p>It goes through the unnormalised components; a subclass that knows how to do it directly
     * overrides it.
     *
     * @throws UnsupportedOperationException if the model cannot assemble the pixel
     */
    public int getDataElement(float[] normComponents, int normOffset) {
        int[] components = this.getUnnormalizedComponents(normComponents, normOffset, null, 0);
        return this.getDataElement(components, 0);
    }

    /**
     * Normalised components brought into a raw pixel.
     *
     * @throws UnsupportedOperationException if the model cannot assemble the pixel
     */
    public Object getDataElements(float[] normComponents, int normOffset, Object obj) {
        int[] components = this.getUnnormalizedComponents(normComponents, normOffset, null, 0);
        return this.getDataElements(components, 0, obj);
    }

    /**
     * The normalised components of a raw pixel.
     *
     * @throws UnsupportedOperationException if the model does not know how to separate the
     *     components
     */
    public float[] getNormalizedComponents(Object pixel, float[] normComponents, int normOffset) {
        int[] components = this.getComponents(pixel, null, 0);
        return this.getNormalizedComponents(components, 0, normComponents, normOffset);
    }

    /**
     * A sample model that suits this colour model.
     *
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        throw new UnsupportedOperationException("This method is not supported by this color model");
    }

    /**
     * A raster that suits this colour model.
     *
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        throw new UnsupportedOperationException("This method is not supported by this color model");
    }

    /**
     * Whether that sample model suits this one.
     *
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        throw new UnsupportedOperationException("This method is not supported by this color model");
    }

    /**
     * Whether that raster suits this one.
     *
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public boolean isCompatibleRaster(Raster raster) {
        throw new UnsupportedOperationException(
                "This method has not been implemented for this ColorModel.");
    }

    /**
     * The alpha channel of the raster, as a one-band raster **over the same data**.
     *
     * <p>It returns `null` when the model has no alpha or when the alpha does not live in a
     * separate band that can be seen on its own. It is the honest answer and not an error: there
     * are formats where the alpha exists but not as a band.
     */
    public WritableRaster getAlphaRaster(WritableRaster raster) {
        return null;
    }

    /**
     * Changes the raster to premultiplied alpha, or back, and returns the model that corresponds.
     *
     * <p>It modifies the raster **in place**.
     *
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public ColorModel coerceData(WritableRaster raster, boolean isAlphaPremultiplied) {
        throw new UnsupportedOperationException("This method is not supported by this color model");
    }

    /** Equality by class, size, colour space and alpha flag. */
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ColorModel cm = (ColorModel) obj;
        if (this.supportsAlpha != cm.supportsAlpha
                || this.isAlphaPremultiplied != cm.isAlphaPremultiplied
                || this.pixel_bits != cm.pixel_bits
                || this.transparency != cm.transparency
                || this.numComponents != cm.numComponents
                || this.transferType != cm.transferType) {
            return false;
        }
        if (this.colorSpace == null) {
            if (cm.colorSpace != null) {
                return false;
            }
        } else if (!this.colorSpace.equals(cm.colorSpace)) {
            return false;
        }
        if (this.nBits == null) {
            return cm.nBits == null;
        }
        if (cm.nBits == null || this.nBits.length != cm.nBits.length) {
            return false;
        }
        for (int i = 0; i < this.nBits.length; i++) {
            if (this.nBits[i] != cm.nBits[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = this.supportsAlpha ? 2 : 3;
        h = 31 * h + (this.isAlphaPremultiplied ? 1 : 0);
        h = 31 * h + this.pixel_bits;
        h = 31 * h + this.transparency;
        h = 31 * h + this.numComponents;
        if (this.nBits != null) {
            for (int i = 0; i < this.nBits.length; i++) {
                h = 31 * h + this.nBits[i];
            }
        }
        h = 31 * h + this.transferType;
        return h;
    }

    public String toString() {
        return "ColorModel: #pixelBits = " + this.pixel_bits + " numComponents = "
                + this.numComponents + " color space = " + this.colorSpace
                + " transparency = " + this.transparency + " has alpha = " + this.supportsAlpha
                + " isAlphaPre = " + this.isAlphaPremultiplied;
    }
}
