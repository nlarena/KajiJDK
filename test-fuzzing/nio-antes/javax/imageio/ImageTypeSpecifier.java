package javax.imageio;

import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

/**
 * KajiLibrary's javax.imageio.ImageTypeSpecifier -- what type an image is or will be.
 *
 * <p>A {@link ColorModel} plus a {@link SampleModel}, <b>without a size</b>. That absence is the
 * whole point of the class: it describes the <i>format</i> of the pixels and not a concrete
 * image.
 *
 * <p>That is why it serves for what a {@code BufferedImage} cannot: asking a reader "which formats
 * can you give me this image in" before decoding anything, or telling a writer "write it in this
 * format". Allocating a whole image to answer that would be absurd.
 *
 * <h2>The seven factories</h2>
 *
 * <p>Building the pair by hand is easy to get wrong, hence a factory for each usual pixel layout:
 *
 * <ul>
 *   <li>{@link #createInterleaved}: the bands interleaved in one array, {@code RGBRGBRGB};
 *   <li>{@link #createBanded}: one band per array, {@code RRR GGG BBB};
 *   <li>{@link #createPacked}: several bands packed into one integer per pixel, with masks;
 *   <li>{@link #createGrayscale}: a single grey band;
 *   <li>{@link #createIndexed}: a colour table and one index per pixel;
 *   <li>{@link #createFromBufferedImageType} and {@link #createFromRenderedImage}: copying the type
 *       of something that already exists.
 * </ul>
 *
 * <p>The difference between interleaved and banded looks cosmetic and is not: reading a whole
 * channel is one contiguous pass in the second and jumps in the first.
 *
 * <h2>{@link #getBufferedImageType}</h2>
 *
 * <p>It returns one of the {@code TYPE_} constants of {@link BufferedImage}, or
 * {@link BufferedImage#TYPE_CUSTOM} if the pair matches none.
 *
 * <p>And {@code TYPE_CUSTOM} is normal, not a failure: the named types are a handful of frequent
 * cases, and anything slightly different --sRGB interleaved in RGB order, for example-- falls into
 * custom.
 */
public class ImageTypeSpecifier {

    /** How the pixels are interpreted. */
    protected ColorModel colorModel;

    /** How they are laid out. */
    protected SampleModel sampleModel;

    /**
     * The pair, directly.
     *
     * @throws IllegalArgumentException if either is null, or if they are not compatible
     */
    public ImageTypeSpecifier(ColorModel colorModel, SampleModel sampleModel) {
        if (colorModel == null) {
            throw new IllegalArgumentException("colorModel == null!");
        }
        if (sampleModel == null) {
            throw new IllegalArgumentException("sampleModel == null!");
        }
        if (!colorModel.isCompatibleSampleModel(sampleModel)) {
            throw new IllegalArgumentException("sampleModel is incompatible with colorModel!");
        }
        this.colorModel = colorModel;
        this.sampleModel = sampleModel;
    }

    /**
     * The type of that image.
     *
     * @throws IllegalArgumentException if it is null
     */
    public ImageTypeSpecifier(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        this.colorModel = image.getColorModel();
        this.sampleModel = image.getSampleModel();
    }

    /**
     * Bands packed into one integer per pixel. See the class note.
     *
     * @param redMask which bits are red
     * @param alphaMask which bits are alpha, or 0 if there is none
     * @param transferType {@link DataBuffer#TYPE_BYTE}, {@code TYPE_USHORT} or {@code TYPE_INT}
     * @param isAlphaPremultiplied whether the colour already comes multiplied by the alpha
     * @throws IllegalArgumentException if the colour space does not have three components, if the
     *     masks are wrong, or if the transfer type does not work
     */
    public static ImageTypeSpecifier createPacked(ColorSpace colorSpace, int redMask,
                                                  int greenMask, int blueMask, int alphaMask,
                                                  int transferType,
                                                  boolean isAlphaPremultiplied) {
        if (colorSpace == null) {
            throw new IllegalArgumentException("colorSpace == null!");
        }
        if (colorSpace.getType() != ColorSpace.TYPE_RGB) {
            throw new IllegalArgumentException("colorSpace is not of type TYPE_RGB!");
        }
        if (transferType != DataBuffer.TYPE_BYTE && transferType != DataBuffer.TYPE_USHORT
            && transferType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Bad value for transferType!");
        }
        if (redMask == 0 && greenMask == 0 && blueMask == 0 && alphaMask == 0) {
            throw new IllegalArgumentException("No mask has at least 1 bit set!");
        }
        int bits = 32;
        ColorModel colorModel = new DirectColorModel(colorSpace, bits, redMask, greenMask,
                                                     blueMask, alphaMask, isAlphaPremultiplied,
                                                     transferType);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(1, 1);
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    /**
     * Bands interleaved in one array. See the class note.
     *
     * @param bandOffsets in which order the bands come
     * @param hasAlpha whether the last band is alpha
     * @throws IllegalArgumentException if something does not add up
     */
    public static ImageTypeSpecifier createInterleaved(ColorSpace colorSpace, int[] bandOffsets,
                                                       int dataType, boolean hasAlpha,
                                                       boolean isAlphaPremultiplied) {
        if (colorSpace == null) {
            throw new IllegalArgumentException("colorSpace == null!");
        }
        if (bandOffsets == null) {
            throw new IllegalArgumentException("bandOffsets == null!");
        }
        int numBands = bandOffsets.length;
        int numComponents = colorSpace.getNumComponents();
        if (hasAlpha) {
            numComponents = numComponents + 1;
        }
        if (numBands != numComponents) {
            throw new IllegalArgumentException(
                "bandOffsets.length is wrong for colorSpace!");
        }
        int transparency;
        if (hasAlpha) {
            transparency = java.awt.Transparency.TRANSLUCENT;
        } else {
            transparency = java.awt.Transparency.OPAQUE;
        }
        int[] bits = new int[numBands];
        int size = DataBuffer.getDataTypeSize(dataType);
        int i = 0;
        while (i < numBands) {
            bits[i] = size;
            i = i + 1;
        }
        ColorModel colorModel = new ComponentColorModel(colorSpace, bits, hasAlpha,
                                                        isAlphaPremultiplied, transparency,
                                                        dataType);
        int minBandOffset = bandOffsets[0];
        int maxBandOffset = bandOffsets[0];
        i = 0;
        while (i < bandOffsets.length) {
            if (bandOffsets[i] < minBandOffset) {
                minBandOffset = bandOffsets[i];
            }
            if (bandOffsets[i] > maxBandOffset) {
                maxBandOffset = bandOffsets[i];
            }
            i = i + 1;
        }
        int pixelStride = maxBandOffset - minBandOffset + 1;
        pixelStride = Math.max(pixelStride, bandOffsets.length);
        SampleModel sampleModel = new PixelInterleavedSampleModel(dataType, 1, 1, pixelStride,
                                                                  pixelStride, bandOffsets);
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    /**
     * One band per array. See the class note.
     *
     * @throws IllegalArgumentException if something does not add up
     */
    public static ImageTypeSpecifier createBanded(ColorSpace colorSpace, int[] bankIndices,
                                                  int[] bandOffsets, int dataType,
                                                  boolean hasAlpha,
                                                  boolean isAlphaPremultiplied) {
        if (colorSpace == null) {
            throw new IllegalArgumentException("colorSpace == null!");
        }
        if (bankIndices == null) {
            throw new IllegalArgumentException("bankIndices == null!");
        }
        if (bandOffsets == null) {
            throw new IllegalArgumentException("bandOffsets == null!");
        }
        if (bankIndices.length != bandOffsets.length) {
            throw new IllegalArgumentException(
                "bankIndices.length != bandOffsets.length!");
        }
        int numBands = bandOffsets.length;
        int numComponents = colorSpace.getNumComponents();
        if (hasAlpha) {
            numComponents = numComponents + 1;
        }
        if (numBands != numComponents) {
            throw new IllegalArgumentException("bandOffsets.length is wrong for colorSpace!");
        }
        int transparency;
        if (hasAlpha) {
            transparency = java.awt.Transparency.TRANSLUCENT;
        } else {
            transparency = java.awt.Transparency.OPAQUE;
        }
        int[] bits = new int[numBands];
        int size = DataBuffer.getDataTypeSize(dataType);
        int i = 0;
        while (i < numBands) {
            bits[i] = size;
            i = i + 1;
        }
        ColorModel colorModel = new ComponentColorModel(colorSpace, bits, hasAlpha,
                                                        isAlphaPremultiplied, transparency,
                                                        dataType);
        SampleModel sampleModel = new BandedSampleModel(dataType, 1, 1, 1, bankIndices,
                                                        bandOffsets);
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    /** A single grey band, without alpha. */
    public static ImageTypeSpecifier createGrayscale(int bits, int dataType, boolean isSigned) {
        return createGrayscale(bits, dataType, isSigned, false);
    }

    /**
     * A single grey band.
     *
     * <p>With 1, 2 or 4 bits and no alpha it uses a {@link MultiPixelPackedSampleModel}: several
     * pixels per byte, which is how one-bit images are stored.
     *
     * @throws IllegalArgumentException if the bits are not 1, 2, 4, 8, 16 or 32, or do not fit in
     *     the type (the JDK does not accept 32)
     */
    public static ImageTypeSpecifier createGrayscale(int bits, int dataType, boolean isSigned,
                                                     boolean isAlphaPremultiplied) {
        if (bits != 1 && bits != 2 && bits != 4 && bits != 8 && bits != 16 && bits != 32) {
            throw new IllegalArgumentException("Bad value for bits!");
        }
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_SHORT
            && dataType != DataBuffer.TYPE_USHORT && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Bad value for dataType!");
        }
        if (bits > DataBuffer.getDataTypeSize(dataType)) {
            throw new IllegalArgumentException("Too many bits for dataType!");
        }
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        int numBands = 1;
        boolean hasAlpha = false;
        int transparency = java.awt.Transparency.OPAQUE;
        ColorModel colorModel = new ComponentColorModel(colorSpace, new int[] { bits },
                                                        hasAlpha, isAlphaPremultiplied,
                                                        transparency, dataType);
        SampleModel sampleModel;
        if (bits < 8 && numBands == 1) {
            // Several pixels per byte: it is what makes a one-bit image take an eighth.
            sampleModel = new MultiPixelPackedSampleModel(dataType, 1, 1, bits);
        } else {
            sampleModel = new PixelInterleavedSampleModel(dataType, 1, 1, numBands, numBands,
                                                          new int[] { 0 });
        }
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    /**
     * A colour table and one index per pixel.
     *
     * @param redLUT the red component of each entry
     * @param alphaLUT the alpha of each entry, or null for opaque
     * @param bits how many bits per index: 1, 2, 4, 8 or 16
     * @throws IllegalArgumentException if the tables do not have the same length, if the bits do
     *     not work, or if the table is bigger than the bits allow
     */
    public static ImageTypeSpecifier createIndexed(byte[] redLUT, byte[] greenLUT, byte[] blueLUT,
                                                   byte[] alphaLUT, int bits, int dataType) {
        if (redLUT == null || greenLUT == null || blueLUT == null) {
            throw new IllegalArgumentException("LUT is null!");
        }
        if (bits != 1 && bits != 2 && bits != 4 && bits != 8 && bits != 16) {
            throw new IllegalArgumentException("Bad value for bits!");
        }
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_SHORT
            && dataType != DataBuffer.TYPE_USHORT && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Bad value for dataType!");
        }
        int len = 1 << bits;
        if (redLUT.length != greenLUT.length || redLUT.length != blueLUT.length
            || (alphaLUT != null && redLUT.length != alphaLUT.length)) {
            throw new IllegalArgumentException("LUTs have different lengths!");
        }
        if (redLUT.length > len) {
            throw new IllegalArgumentException("LUT has improper length!");
        }
        ColorModel colorModel;
        if (alphaLUT == null) {
            colorModel = new IndexColorModel(bits, redLUT.length, redLUT, greenLUT, blueLUT);
        } else {
            colorModel = new IndexColorModel(bits, redLUT.length, redLUT, greenLUT, blueLUT,
                                             alphaLUT);
        }
        SampleModel sampleModel;
        if (bits == 8) {
            int[] bandOffsets = new int[1];
            sampleModel = new PixelInterleavedSampleModel(dataType, 1, 1, 1, 1, bandOffsets);
        } else {
            sampleModel = new MultiPixelPackedSampleModel(dataType, 1, 1, bits);
        }
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    /**
     * The type of one of {@link BufferedImage}'s {@code TYPE_} constants.
     *
     * @throws IllegalArgumentException if it is {@link BufferedImage#TYPE_CUSTOM} or not a constant
     */
    public static ImageTypeSpecifier createFromBufferedImageType(int bufferedImageType) {
        if (bufferedImageType == BufferedImage.TYPE_CUSTOM) {
            throw new IllegalArgumentException("Cannot create from TYPE_CUSTOM!");
        }
        if (bufferedImageType < BufferedImage.TYPE_CUSTOM
            || bufferedImageType > BufferedImage.TYPE_BYTE_INDEXED) {
            throw new IllegalArgumentException("Invalid BufferedImage type!");
        }
        // Build a one-pixel image and take its pair: replicating the thirteen combinations by
        // hand would duplicate what BufferedImage already knows, and drift out of sync with it.
        BufferedImage bi = new BufferedImage(1, 1, bufferedImageType);
        return new ImageTypeSpecifier(bi);
    }

    /**
     * The type of that image.
     *
     * @throws IllegalArgumentException if it is null
     */
    public static ImageTypeSpecifier createFromRenderedImage(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        return new ImageTypeSpecifier(image);
    }

    /**
     * Which of the {@code TYPE_} constants, or {@link BufferedImage#TYPE_CUSTOM}.
     *
     * <p>See the class note: custom is normal.
     */
    public int getBufferedImageType() {
        BufferedImage bi = createBufferedImage(1, 1);
        return bi.getType();
    }

    /** How many components the colour model has. */
    public int getNumComponents() {
        return this.colorModel.getNumComponents();
    }

    /** How many bands the sample model has. */
    public int getNumBands() {
        return this.sampleModel.getNumBands();
    }

    /**
     * How many bits that band has.
     *
     * @throws IllegalArgumentException if the band does not exist
     */
    public int getBitsPerBand(int band) {
        if (band < 0 || band >= getNumBands()) {
            throw new IllegalArgumentException("band out of range!");
        }
        return this.sampleModel.getSampleSize(band);
    }

    /** The sample model, of one pixel. */
    public SampleModel getSampleModel() {
        return this.sampleModel;
    }

    /**
     * The sample model at that size.
     *
     * @throws IllegalArgumentException if the width or height are not positive
     * @throws IllegalArgumentException if the product overflows
     */
    public SampleModel getSampleModel(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width or height <= 0!");
        }
        if ((long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("width * height > Integer.MAX_VALUE!");
        }
        return this.sampleModel.createCompatibleSampleModel(width, height);
    }

    /** The colour model. */
    public ColorModel getColorModel() {
        return this.colorModel;
    }

    /**
     * An empty image of that size and this type.
     *
     * @throws IllegalArgumentException if the width or height are not positive, or if the product
     *     overflows
     */
    public BufferedImage createBufferedImage(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width or height <= 0!");
        }
        if ((long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("width * height > Integer.MAX_VALUE!");
        }
        SampleModel sm = this.sampleModel.createCompatibleSampleModel(width, height);
        WritableRaster raster = Raster.createWritableRaster(sm, new java.awt.Point(0, 0));
        return new BufferedImage(this.colorModel, raster,
                                 this.colorModel.isAlphaPremultiplied(), null);
    }

    /**
     * Equal if the colour model and the sample model are equal.
     *
     * <p>The size of the sample model does not count: two types with the same format are the same
     * type, even if their sample models were built for different sizes.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ImageTypeSpecifier)) {
            return false;
        }
        ImageTypeSpecifier that = (ImageTypeSpecifier) o;
        return this.colorModel.equals(that.colorModel)
            && this.sampleModel.equals(that.sampleModel);
    }

    /** Consistent with {@link #equals}. */
    @Override
    public int hashCode() {
        return 9 * this.colorModel.hashCode() + 14 * this.sampleModel.hashCode();
    }
}
