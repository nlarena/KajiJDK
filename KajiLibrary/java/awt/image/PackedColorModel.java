package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;

/**
 * A colour model whose components are **bit fields** inside a single pixel.
 *
 * <p>It is the colour half of what {@link SinglePixelPackedSampleModel} is of layout, and the two
 * classes are declared the same way: by masks. Out of each mask come the shift and the width of its
 * component, and out of those everything else.
 *
 * <p>A mask has to be **contiguous** —a single run of ones— and cannot go past the bits of the
 * pixel. The first part is what lets the component be read with a shift and a logical `and` instead
 * of having to join loose pieces.
 *
 * <p>The constructor may end up lowering the declared transparency to `BITMASK`: if the alpha was
 * left with a single bit, the pixel can only be fully opaque or fully transparent, and saying
 * `TRANSLUCENT` would be promising something the format cannot give. That is only known after
 * taking the masks apart, which is why it is corrected afterwards and not in the argument list.
 */
public abstract class PackedColorModel extends ColorModel {

    int[] maskArray;
    int[] maskOffsets;
    float[] scaleFactors;

    /**
     * With the colour masks in an array and the alpha one apart.
     *
     * <p>It serves for colour spaces of any number of components; the other constructor is the
     * shortcut for RGB.
     *
     * @throws IllegalArgumentException if `bits` is not between 1 and 32, if some mask is not
     *     contiguous, or if some mask goes past the bits of the pixel
     * @throws NullPointerException if the colour space is missing
     */
    public PackedColorModel(ColorSpace space, int bits, int[] colorMaskArray, int alphaMask,
            boolean isAlphaPremultiplied, int trans, int transferType) {
        super(bits, createBitsArray(colorMaskArray, alphaMask), space, alphaMask != 0,
                isAlphaPremultiplied, trans, transferType);
        if (bits < 1 || bits > 32) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 32.");
        }
        this.maskArray = new int[this.numComponents];
        this.maskOffsets = new int[this.numComponents];
        this.scaleFactors = new float[this.numComponents];
        for (int i = 0; i < this.numColorComponents; i++) {
            this.decomposeMask(colorMaskArray[i], i, space.getName(i));
        }
        if (alphaMask != 0) {
            this.decomposeMask(alphaMask, this.numColorComponents, "alpha");
            if (this.nBits[this.numComponents - 1] == 1) {
                this.transparency = Transparency.BITMASK;
            }
        }
    }

    /**
     * The shortcut for RGB: the three colour masks and the alpha one separately.
     *
     * @throws IllegalArgumentException if the space is not of RGB type, if `bits` is not between 1
     *     and 32, if some mask is not contiguous, or if some mask goes past the bits of the pixel
     * @throws NullPointerException if the colour space is missing
     */
    public PackedColorModel(ColorSpace space, int bits, int rmask, int gmask, int bmask, int amask,
            boolean isAlphaPremultiplied, int trans, int transferType) {
        super(bits, createBitsArray(rmask, gmask, bmask, amask), space, amask != 0,
                isAlphaPremultiplied, trans, transferType);
        if (space.getType() != ColorSpace.TYPE_RGB) {
            throw new IllegalArgumentException("ColorSpace must be TYPE_RGB.");
        }
        if (bits < 1 || bits > 32) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 32.");
        }
        this.maskArray = new int[this.numComponents];
        this.maskOffsets = new int[this.numComponents];
        this.scaleFactors = new float[this.numComponents];
        this.decomposeMask(rmask, 0, "red");
        this.decomposeMask(gmask, 1, "green");
        this.decomposeMask(bmask, 2, "blue");
        if (amask != 0) {
            this.decomposeMask(amask, 3, "alpha");
            if (this.nBits[3] == 1) {
                this.transparency = Transparency.BITMASK;
            }
        }
    }

    /**
     * How many bits the mask has, or -1 if it is not contiguous.
     *
     * <p>The zeros at the bottom are shifted out, then the ones, and if anything is left on there
     * was a hole: the mask had two runs.
     */
    private static int countBits(int mask) {
        int m = mask;
        int count = 0;
        if (m != 0) {
            while ((m & 1) == 0) {
                m = m >>> 1;
            }
            while ((m & 1) == 1) {
                m = m >>> 1;
                count = count + 1;
            }
        }
        if (m != 0) {
            return -1;
        }
        return count;
    }

    /**
     * The component widths that come out of the masks.
     *
     * @throws IllegalArgumentException if some mask is not contiguous
     */
    private static int[] createBitsArray(int[] colorMaskArray, int alphaMask) {
        int numColors = colorMaskArray.length;
        int numAlpha = alphaMask == 0 ? 0 : 1;
        int[] arr = new int[numColors + numAlpha];
        for (int i = 0; i < numColors; i++) {
            arr[i] = countBits(colorMaskArray[i]);
            if (arr[i] < 0) {
                throw new IllegalArgumentException("Noncontiguous color mask ("
                        + Integer.toHexString(colorMaskArray[i]) + "at index " + i);
            }
        }
        if (alphaMask != 0) {
            arr[numColors] = countBits(alphaMask);
            if (arr[numColors] < 0) {
                throw new IllegalArgumentException("Noncontiguous alpha mask ("
                        + Integer.toHexString(alphaMask));
            }
        }
        return arr;
    }

    /**
     * The same for the RGB shortcut.
     *
     * @throws IllegalArgumentException if some mask is not contiguous
     */
    private static int[] createBitsArray(int rmask, int gmask, int bmask, int amask) {
        int[] arr = new int[3 + (amask == 0 ? 0 : 1)];
        arr[0] = countBits(rmask);
        if (arr[0] < 0) {
            throw new IllegalArgumentException("Noncontiguous red mask ("
                    + Integer.toHexString(rmask));
        }
        arr[1] = countBits(gmask);
        if (arr[1] < 0) {
            throw new IllegalArgumentException("Noncontiguous green mask ("
                    + Integer.toHexString(gmask));
        }
        arr[2] = countBits(bmask);
        if (arr[2] < 0) {
            throw new IllegalArgumentException("Noncontiguous blue mask ("
                    + Integer.toHexString(bmask));
        }
        if (amask != 0) {
            arr[3] = countBits(amask);
            if (arr[3] < 0) {
                throw new IllegalArgumentException("Noncontiguous alpha mask ("
                        + Integer.toHexString(amask));
            }
        }
        return arr;
    }

    /**
     * Stores the mask and works out its shift and its scale factor.
     *
     * <p>The factor brings the component to 0..255, which is the scale a colour is asked for in.
     * With eight bits it is exactly 1 and the conversion does nothing.
     *
     * @throws IllegalArgumentException if the mask goes past the bits of the pixel
     */
    private void decomposeMask(int mask, int idx, String componentName) {
        int off = 0;
        int count = this.nBits[idx];
        this.maskArray[idx] = mask;
        int m = mask;
        if (m != 0) {
            while ((m & 1) == 0) {
                m = m >>> 1;
                off = off + 1;
            }
        }
        if (count + off > this.pixel_bits) {
            throw new IllegalArgumentException(componentName + " mask "
                    + Integer.toHexString(this.maskArray[idx]) + " overflows pixel (expecting "
                    + this.pixel_bits + " bits");
        }
        this.maskOffsets[idx] = off;
        if (count == 0) {
            this.scaleFactors[idx] = 256.0f;
        } else {
            this.scaleFactors[idx] = 255.0f / ((1 << count) - 1);
        }
    }

    /**
     * The mask of that component.
     *
     * @throws ArrayIndexOutOfBoundsException if the component does not exist
     */
    public final int getMask(int index) {
        return this.maskArray[index];
    }

    /** The masks of every component. */
    public final int[] getMasks() {
        return this.maskArray.clone();
    }

    /** A {@link SinglePixelPackedSampleModel} with these same masks. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        return new SinglePixelPackedSampleModel(this.transferType, w, h, this.maskArray);
    }

    /**
     * Whether that sample model uses exactly these masks.
     *
     * <p>The masks are compared **trimmed to the transfer type**: the bits the type cannot store do
     * not tell apart two models that behave the same way.
     */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        if (!(sm instanceof SinglePixelPackedSampleModel)) {
            return false;
        }
        if (this.numComponents != sm.getNumBands()) {
            return false;
        }
        if (sm.getTransferType() != this.transferType) {
            return false;
        }
        SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel) sm;
        int[] bitMasks = sppsm.getBitMasks();
        if (bitMasks.length != this.maskArray.length) {
            return false;
        }
        int maxMask = (int) ((1L << DataBuffer.getDataTypeSize(this.transferType)) - 1);
        for (int i = 0; i < bitMasks.length; i++) {
            if ((maxMask & bitMasks[i]) != (maxMask & this.maskArray[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * The alpha channel as a one-band raster **over the same data**.
     *
     * <p>It returns `null` if the model has no alpha. In this format the alpha is always the last
     * band, so the view is built with a one-band child.
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

    /** The equality of {@link ColorModel} plus the masks. */
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        PackedColorModel cm = (PackedColorModel) obj;
        for (int i = 0; i < this.maskArray.length; i++) {
            if (this.maskArray[i] != cm.maskArray[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = super.hashCode();
        for (int i = 0; i < this.maskArray.length; i++) {
            h = 31 * h + this.maskArray[i];
        }
        return h;
    }
}
