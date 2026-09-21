package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;

/**
 * The colour model of a screen: red, green, blue and optionally alpha, in bit fields of one same
 * pixel.
 *
 * <p>It is the concrete case of {@link PackedColorModel} for RGB, and by far the most used: the
 * 32-bit ARGB that {@link ColorModel#getRGBdefault} returns is an instance of this class, and so
 * are the 16-bit 565 and the 24-bit RGB.
 *
 * <p>Every conversion goes through the same chain: take the component out with its mask, bring it
 * to 0..1 by dividing by its own maximum, undo the premultiplication if there is one, and only then
 * convert. When the space is sRGB —the normal case— that conversion is a multiplication by 255;
 * when it is not, one has to go through {@link ColorSpace#toRGB}, which is what makes a model in
 * another space go on answering correctly how red a pixel is.
 *
 * <p>The space has to be of RGB type. It is not a restriction of this class but of its API: the one
 * of three masks called red, green and blue means nothing in a space that does not have them.
 */
public class DirectColorModel extends PackedColorModel {

    /**
     * An opaque RGB model in sRGB.
     *
     * @throws IllegalArgumentException if `bits` is not between 1 and 32 or if some mask is not
     *     contiguous
     */
    public DirectColorModel(int bits, int rmask, int gmask, int bmask) {
        this(bits, rmask, gmask, bmask, 0);
    }

    /**
     * An RGB model in sRGB, with alpha if `amask` is not zero.
     *
     * @throws IllegalArgumentException if `bits` is not between 1 and 32 or if some mask is not
     *     contiguous
     */
    public DirectColorModel(int bits, int rmask, int gmask, int bmask, int amask) {
        super(ColorSpace.getInstance(ColorSpace.CS_sRGB), bits, rmask, gmask, bmask, amask, false,
                amask == 0 ? Transparency.OPAQUE : Transparency.TRANSLUCENT,
                ColorModel.getDefaultTransferType(bits));
    }

    /**
     * The general constructor: any RGB space, with or without premultiplied alpha.
     *
     * @throws IllegalArgumentException if the space is not of RGB type, if `bits` is not between 1
     *     and 32, or if some mask is not contiguous
     */
    public DirectColorModel(ColorSpace space, int bits, int rmask, int gmask, int bmask, int amask,
            boolean isAlphaPremultiplied, int transferType) {
        super(space, bits, rmask, gmask, bmask, amask, isAlphaPremultiplied,
                amask == 0 ? Transparency.OPAQUE : Transparency.TRANSLUCENT, transferType);
    }

    /** The mask of the red. */
    public final int getRedMask() {
        return this.maskArray[0];
    }

    /** The mask of the green. */
    public final int getGreenMask() {
        return this.maskArray[1];
    }

    /** The mask of the blue. */
    public final int getBlueMask() {
        return this.maskArray[2];
    }

    /** The mask of the alpha, or 0 if it has none. */
    public final int getAlphaMask() {
        if (this.supportsAlpha) {
            return this.maskArray[3];
        }
        return 0;
    }

    /** The raw component, just as it is stored. */
    private int rawComponent(int pixel, int idx) {
        return (pixel & this.maskArray[idx]) >>> this.maskOffsets[idx];
    }

    /** The alpha of the pixel, from 0 to 1. */
    private float normalizedAlpha(int pixel) {
        if (!this.supportsAlpha) {
            return 1.0f;
        }
        int a = this.rawComponent(pixel, 3);
        return ((float) a) / ((float) ((1 << this.nBits[3]) - 1));
    }

    /**
     * A colour component from 0 to 1, with the premultiplication already undone.
     *
     * <p>With alpha zero there is no colour to recover: the pixel is invisible and all that can be
     * said is zero. Dividing anyway would give infinity or NaN, which is not a colour.
     */
    private float normalizedColor(int pixel, int idx) {
        float c = ((float) this.rawComponent(pixel, idx)) / ((float) ((1 << this.nBits[idx]) - 1));
        if (this.isAlphaPremultiplied) {
            float a = this.normalizedAlpha(pixel);
            if (a == 0.0f) {
                return 0.0f;
            }
            return c / a;
        }
        return c;
    }

    /**
     * One of the three sRGB components of the pixel, from 0 to 255.
     *
     * <p>If the space is sRGB already there is nothing to convert and scaling is enough. If it is
     * not, the three have to go through the space together: the sRGB red of a pixel in another
     * space depends on its three components, not only on the first.
     */
    private int enSrgb(int pixel, int idx) {
        if (this.isSrgb) {
            return (int) (this.normalizedColor(pixel, idx) * 255.0f + 0.5f);
        }
        float[] comps = new float[this.numColorComponents];
        for (int i = 0; i < this.numColorComponents; i++) {
            float min = this.colorSpace.getMinValue(i);
            float max = this.colorSpace.getMaxValue(i);
            comps[i] = min + this.normalizedColor(pixel, i) * (max - min);
        }
        float[] rgb = this.colorSpace.toRGB(comps);
        float v = rgb[idx];
        if (v < 0.0f) {
            v = 0.0f;
        }
        if (v > 1.0f) {
            v = 1.0f;
        }
        return (int) (v * 255.0f + 0.5f);
    }

    /** The red of the pixel, from 0 to 255 and in sRGB. */
    public final int getRed(int pixel) {
        return this.enSrgb(pixel, 0);
    }

    /** The green of the pixel, from 0 to 255 and in sRGB. */
    public final int getGreen(int pixel) {
        return this.enSrgb(pixel, 1);
    }

    /** The blue of the pixel, from 0 to 255 and in sRGB. */
    public final int getBlue(int pixel) {
        return this.enSrgb(pixel, 2);
    }

    /** The alpha of the pixel, from 0 to 255; 255 if the model has no alpha. */
    public final int getAlpha(int pixel) {
        if (!this.supportsAlpha) {
            return 255;
        }
        return (int) (this.normalizedAlpha(pixel) * 255.0f + 0.5f);
    }

    /** The whole pixel as ARGB with eight bits per channel. */
    public final int getRGB(int pixel) {
        return (this.getAlpha(pixel) << 24) | (this.getRed(pixel) << 16)
                | (this.getGreen(pixel) << 8) | this.getBlue(pixel);
    }

    /**
     * A raw pixel brought into an `int`.
     *
     * @throws UnsupportedOperationException if the type does not fit in an `int`
     * @throws ClassCastException if the array is not of the transfer type
     */
    private int fromRaw(Object inData) {
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            return ((byte[]) inData)[0] & 0xFF;
        }
        if (this.transferType == DataBuffer.TYPE_USHORT) {
            return ((short[]) inData)[0] & 0xFFFF;
        }
        if (this.transferType == DataBuffer.TYPE_INT) {
            return ((int[]) inData)[0];
        }
        throw new UnsupportedOperationException(
                "This method has not been implemented for transferType " + this.transferType);
    }

    /**
     * An `int` stored in an array of the transfer type.
     *
     * @throws UnsupportedOperationException if the type does not fit in an `int`
     */
    private Object toRaw(int pixel, Object obj) {
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            byte[] out = obj == null ? new byte[1] : (byte[]) obj;
            out[0] = (byte) pixel;
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_USHORT) {
            short[] out = obj == null ? new short[1] : (short[]) obj;
            out[0] = (short) pixel;
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_INT) {
            int[] out = obj == null ? new int[1] : (int[]) obj;
            out[0] = pixel;
            return out;
        }
        throw new UnsupportedOperationException(
                "This method has not been implemented for transferType " + this.transferType);
    }

    /**
     * The red of a raw pixel.
     *
     * @throws UnsupportedOperationException if the type does not fit in an `int`
     */
    public int getRed(Object inData) {
        return this.getRed(this.fromRaw(inData));
    }

    /**
     * The green of a raw pixel.
     *
     * @throws UnsupportedOperationException if the type does not fit in an `int`
     */
    public int getGreen(Object inData) {
        return this.getGreen(this.fromRaw(inData));
    }

    /**
     * The blue of a raw pixel.
     *
     * @throws UnsupportedOperationException if the type does not fit in an `int`
     */
    public int getBlue(Object inData) {
        return this.getBlue(this.fromRaw(inData));
    }

    /**
     * The alpha of a raw pixel.
     *
     * @throws UnsupportedOperationException if the type does not fit in an `int`
     */
    public int getAlpha(Object inData) {
        return this.getAlpha(this.fromRaw(inData));
    }

    /**
     * A raw pixel as ARGB with eight bits per channel.
     *
     * @throws UnsupportedOperationException if the type does not fit in an `int`
     */
    public int getRGB(Object inData) {
        return this.getRGB(this.fromRaw(inData));
    }

    /**
     * An ARGB brought into a pixel of this model.
     *
     * @throws UnsupportedOperationException if the type does not fit in an `int`
     */
    public Object getDataElements(int rgb, Object pixel) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        float a = (rgb >>> 24) / 255.0f;
        float[] norm = new float[this.numColorComponents];
        if (this.isSrgb) {
            norm[0] = r;
            norm[1] = g;
            norm[2] = b;
        } else {
            float[] rgbf = new float[3];
            rgbf[0] = r;
            rgbf[1] = g;
            rgbf[2] = b;
            float[] comps = this.colorSpace.fromRGB(rgbf);
            for (int i = 0; i < this.numColorComponents; i++) {
                float min = this.colorSpace.getMinValue(i);
                float max = this.colorSpace.getMaxValue(i);
                norm[i] = (comps[i] - min) / (max - min);
            }
        }
        if (this.supportsAlpha && this.isAlphaPremultiplied) {
            for (int i = 0; i < this.numColorComponents; i++) {
                norm[i] = norm[i] * a;
            }
        }
        int intpixel = 0;
        for (int i = 0; i < this.numColorComponents; i++) {
            int v = (int) (norm[i] * ((1 << this.nBits[i]) - 1) + 0.5f);
            intpixel = intpixel | ((v << this.maskOffsets[i]) & this.maskArray[i]);
        }
        if (this.supportsAlpha) {
            int v = (int) (a * ((1 << this.nBits[3]) - 1) + 0.5f);
            intpixel = intpixel | ((v << this.maskOffsets[3]) & this.maskArray[3]);
        }
        return this.toRaw(intpixel, pixel);
    }

    /** The raw components of the pixel, each one in its own scale. */
    public final int[] getComponents(int pixel, int[] components, int offset) {
        int[] out = components;
        if (out == null) {
            out = new int[offset + this.numComponents];
        }
        for (int i = 0; i < this.numComponents; i++) {
            out[offset + i] = this.rawComponent(pixel, i);
        }
        return out;
    }

    /**
     * The raw components of a raw pixel.
     *
     * @throws UnsupportedOperationException if the type does not fit in an `int`
     */
    public final int[] getComponents(Object pixel, int[] components, int offset) {
        return this.getComponents(this.fromRaw(pixel), components, offset);
    }

    /**
     * Raw components brought into a pixel.
     *
     * @throws IllegalArgumentException if the array does not carry every component
     */
    public int getDataElement(int[] components, int offset) {
        if (components.length - offset < this.numComponents) {
            throw new IllegalArgumentException("Incorrect number of components.  Expecting "
                    + this.numComponents);
        }
        int intpixel = 0;
        for (int i = 0; i < this.numComponents; i++) {
            intpixel = intpixel
                    | ((components[offset + i] << this.maskOffsets[i]) & this.maskArray[i]);
        }
        return intpixel;
    }

    /**
     * Raw components brought into a raw pixel.
     *
     * @throws IllegalArgumentException if the array does not carry every component
     * @throws UnsupportedOperationException if the type does not fit in an `int`
     */
    public Object getDataElements(int[] components, int offset, Object obj) {
        return this.toRaw(this.getDataElement(components, offset), obj);
    }

    /**
     * A packed raster with these masks.
     *
     * @throws IllegalArgumentException if the size is empty
     */
    public final WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") cannot be <= 0");
        }
        int[] bandmasks;
        if (this.supportsAlpha) {
            bandmasks = new int[4];
            bandmasks[3] = this.maskArray[3];
        } else {
            bandmasks = new int[3];
        }
        bandmasks[0] = this.maskArray[0];
        bandmasks[1] = this.maskArray[1];
        bandmasks[2] = this.maskArray[2];
        if (this.pixel_bits > 16) {
            return Raster.createPackedRaster(DataBuffer.TYPE_INT, w, h, bandmasks, null);
        }
        if (this.pixel_bits > 8) {
            return Raster.createPackedRaster(DataBuffer.TYPE_USHORT, w, h, bandmasks, null);
        }
        return Raster.createPackedRaster(DataBuffer.TYPE_BYTE, w, h, bandmasks, null);
    }

    /** Whether that raster is packed with exactly these masks. */
    public boolean isCompatibleRaster(Raster raster) {
        SampleModel sm = raster.getSampleModel();
        if (!(sm instanceof SinglePixelPackedSampleModel)) {
            return false;
        }
        SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel) sm;
        if (sppsm.getNumBands() != this.numComponents) {
            return false;
        }
        int[] bitMasks = sppsm.getBitMasks();
        for (int i = 0; i < this.numComponents; i++) {
            if (bitMasks[i] != this.maskArray[i]) {
                return false;
            }
        }
        return raster.getTransferType() == this.transferType;
    }

    /**
     * Premultiplies the raster by its alpha, or undoes it, **in place**.
     *
     * <p>It returns the model that describes the raster after the change; if it was as asked for
     * already, or if there is no alpha to premultiply, it returns itself without touching anything.
     *
     * <p>The operation loses information in one direction: premultiplying a pixel of alpha zero
     * takes it to black, and undoing it afterwards does not bring it back. It belongs to the
     * representation, not to this implementation.
     */
    public final ColorModel coerceData(WritableRaster raster, boolean isAlphaPremultiplied) {
        if (!this.supportsAlpha || this.isAlphaPremultiplied == isAlphaPremultiplied) {
            return this;
        }
        int w = raster.getWidth();
        int h = raster.getHeight();
        int aIdx = this.numColorComponents;
        int alphaMax = (1 << this.nBits[aIdx]) - 1;
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        int[] pixel = null;
        for (int y = minY; y < minY + h; y++) {
            for (int x = minX; x < minX + w; x++) {
                pixel = raster.getPixel(x, y, pixel);
                float normAlpha = ((float) pixel[aIdx]) / ((float) alphaMax);
                if (isAlphaPremultiplied) {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = (int) (pixel[c] * normAlpha + 0.5f);
                    }
                } else if (normAlpha != 0.0f) {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = (int) (pixel[c] / normAlpha + 0.5f);
                    }
                } else {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = 0;
                    }
                }
                raster.setPixel(x, y, pixel);
            }
        }
        return new DirectColorModel(this.colorSpace, this.pixel_bits, this.maskArray[0],
                this.maskArray[1], this.maskArray[2], this.maskArray[3], isAlphaPremultiplied,
                this.transferType);
    }

    public String toString() {
        return "DirectColorModel: rmask=" + Integer.toHexString(this.maskArray[0])
                + " gmask=" + Integer.toHexString(this.maskArray[1])
                + " bmask=" + Integer.toHexString(this.maskArray[2])
                + " amask=" + Integer.toHexString(this.getAlphaMask());
    }
}
