package java.awt.image;

import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Converts the colours from one space to another.
 *
 * <p>It is the operation that makes to red go on being the same red when it passes from one image
 * to another with to different colour space. It does not change the numbers for the sake of
 * changing them: it changes the numbers **precisely so that the colour does not change**.
 *
 * <p>The conversion always goes through CIEXYZ, which is the reference space where colour is
 * defined without depending on any device. Chaining several profiles is chaining those steps: from
 * each one to XYZ and from XYZ to the next.
 *
 * <p>There are four ways of building it and the difference is in where the two ends come from. With
 * to single {@link ColorSpace} the source is put by the image being filtered and that one is the
 * destination; with two, both are fixed and it serves for rasters as well, which have no colour
 * model of their own; with neither, both come from the images.
 *
 * <p>Over to {@link Raster} both spaces have to be declared and with the same number of components
 * as there are bands: to raster is uninterpreted numbers, and without being told what they are
 * there is nothing to convert.
 */
public class ColorConvertOp implements BufferedImageOp, RasterOp {

    private final ColorSpace[] spaces;
    private final ICC_Profile[] profiles;
    private final RenderingHints hints;

    /**
     * With no declared spaces: both come from the images being filtered.
     *
     * <p>It does not serve for rasters.
     */
    public ColorConvertOp(RenderingHints hints) {
        this.spaces = new ColorSpace[0];
        this.profiles = null;
        this.hints = hints;
    }

    /**
     * With the destination space; the source one comes from the image.
     *
     * @throws NullPointerException if the space is `null`
     */
    public ColorConvertOp(ColorSpace cspace, RenderingHints hints) {
        if (cspace == null) {
            throw new NullPointerException("ColorSpace cannot be null");
        }
        this.spaces = new ColorSpace[1];
        this.spaces[0] = cspace;
        this.profiles = null;
        this.hints = hints;
    }

    /**
     * With both spaces declared.
     *
     * @throws NullPointerException if either of the two is missing
     */
    public ColorConvertOp(ColorSpace srcCspace, ColorSpace dstCspace, RenderingHints hints) {
        if (srcCspace == null || dstCspace == null) {
            throw new NullPointerException("ColorSpaces cannot be null");
        }
        this.spaces = new ColorSpace[2];
        this.spaces[0] = srcCspace;
        this.spaces[1] = dstCspace;
        this.profiles = null;
        this.hints = hints;
    }

    /**
     * With to chain of ICC profiles.
     *
     * @throws NullPointerException if the array is `null`
     * @throws IllegalArgumentException if the array is empty
     */
    public ColorConvertOp(ICC_Profile[] profiles, RenderingHints hints) {
        if (profiles == null) {
            throw new NullPointerException("Profiles cannot be null");
        }
        this.profiles = profiles.clone();
        this.spaces = new ColorSpace[profiles.length];
        for (int i = 0; i < profiles.length; i++) {
            this.spaces[i] = new ICC_ColorSpace(profiles[i]);
        }
        this.hints = hints;
    }

    /** The profiles it was built with, or an empty array if it was not built with profiles. */
    public final ICC_Profile[] getICC_Profiles() {
        if (this.profiles == null) {
            return new ICC_Profile[0];
        }
        return this.profiles.clone();
    }

    /**
     * Converts to colour from one space to another going through CIEXYZ.
     *
     * <p>It is where the whole conversion lives: both spaces know how to come and go from XYZ, and
     * composing those two functions is the conversion between them.
     */
    private static float[] convertColor(ColorSpace from, ColorSpace to, float[] color) {
        if (from == to) {
            return color;
        }
        return to.fromCIEXYZ(from.toCIEXYZ(color));
    }

    /** The chain of spaces that has to be crossed, from source to destination. */
    private ColorSpace[] chain(ColorSpace fromSpace, ColorSpace toSpace) {
        if (this.spaces.length <= 1) {
            ColorSpace[] c = new ColorSpace[2];
            c[0] = fromSpace;
            c[1] = this.spaces.length == 1 ? this.spaces[0] : toSpace;
            return c;
        }
        return this.spaces;
    }

    /**
     * Converts the colours of an image.
     *
     * @param dest the destination, or `null` for it to be created
     * @throws IllegalArgumentException if the sizes do not match, or if the destination space
     *     cannot be determined
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dest) {
        BufferedImage target = dest;
        if (target == null) {
            target = this.createCompatibleDestImage(src, null);
        } else if (src.getWidth() != target.getWidth()
                || src.getHeight() != target.getHeight()) {
            throw new IllegalArgumentException("Width or height of BufferedImages do not match");
        }
        ColorSpace[] chain = this.chain(src.getColorModel().getColorSpace(),
                target.getColorModel().getColorSpace());
        ColorModel srcCM = src.getColorModel();
        ColorModel dstCM = target.getColorModel();
        int w = src.getWidth();
        int h = src.getHeight();
        int srcColour = srcCM.getNumColorComponents();
        int dstColour = dstCM.getNumColorComponents();
        float[] norm = new float[srcCM.getNumComponents()];
        float[] color = new float[srcColour];
        float[] out = new float[dstCM.getNumComponents()];
        Object raw = null;
        Object destRaw = null;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                raw = src.getRaster().getDataElements(x, y, raw);
                norm = srcCM.getNormalizedComponents(raw, norm, 0);
                for (int i = 0; i < srcColour; i++) {
                    float min = chain[0].getMinValue(i);
                    float max = chain[0].getMaxValue(i);
                    color[i] = min + norm[i] * (max - min);
                }
                float[] converted = color;
                for (int i = 1; i < chain.length; i++) {
                    converted = convertColor(chain[i - 1], chain[i], converted);
                }
                ColorSpace last = chain[chain.length - 1];
                for (int i = 0; i < dstColour; i++) {
                    float min = last.getMinValue(i);
                    float max = last.getMaxValue(i);
                    float v = (converted[i] - min) / (max - min);
                    if (v < 0.0f) {
                        v = 0.0f;
                    }
                    if (v > 1.0f) {
                        v = 1.0f;
                    }
                    out[i] = v;
                }
                // The alpha is not converted: it is opacity, not colour, and it lives in no space.
                if (dstCM.hasAlpha()) {
                    out[dstColour] = srcCM.hasAlpha() ? norm[srcColour] : 1.0f;
                }
                destRaw = dstCM.getDataElements(out, 0, destRaw);
                target.getRaster().setDataElements(x, y, destRaw);
            }
        }
        return target;
    }

    /**
     * Converts the colours of to raster.
     *
     * @param dest the destination, or `null` for it to be created
     * @throws IllegalArgumentException if exactly two spaces were not declared, if their number of
     *     components does not match the bands, or if the sizes do not match
     */
    public final WritableRaster filter(Raster src, WritableRaster dest) {
        if (this.spaces.length != 2) {
            throw new IllegalArgumentException(
                    "Destination ColorSpace is undefined");
        }
        ColorSpace from = this.spaces[0];
        ColorSpace to = this.spaces[1];
        if (src.getNumBands() != from.getNumComponents()) {
            throw new IllegalArgumentException(
                    "Numbers of source Raster bands and source color space components do not "
                    + "match");
        }
        WritableRaster target = dest;
        if (target == null) {
            target = this.createCompatibleDestRaster(src);
        } else {
            if (src.getWidth() != target.getWidth() || src.getHeight() != target.getHeight()) {
                throw new IllegalArgumentException("Width or height of Rasters do not match");
            }
            if (target.getNumBands() != to.getNumComponents()) {
                throw new IllegalArgumentException("Numbers of destination Raster bands and "
                        + "destination color space components do not match");
            }
        }
        int w = src.getWidth();
        int h = src.getHeight();
        int sb = src.getNumBands();
        int db = target.getNumBands();
        int[] in = new int[sb];
        int[] out = new int[db];
        float[] color = new float[sb];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                in = src.getPixel(src.getMinX() + x, src.getMinY() + y, in);
                for (int i = 0; i < sb; i++) {
                    int max = (1 << src.getSampleModel().getSampleSize(i)) - 1;
                    float min = from.getMinValue(i);
                    float top = from.getMaxValue(i);
                    color[i] = min + (((float) in[i]) / max) * (top - min);
                }
                float[] converted = convertColor(from, to, color);
                for (int i = 0; i < db; i++) {
                    int max = (1 << target.getSampleModel().getSampleSize(i)) - 1;
                    float min = to.getMinValue(i);
                    float top = to.getMaxValue(i);
                    float v = (converted[i] - min) / (top - min);
                    if (v < 0.0f) {
                        v = 0.0f;
                    }
                    if (v > 1.0f) {
                        v = 1.0f;
                    }
                    out[i] = (int) (v * max + 0.5f);
                }
                target.setPixel(target.getMinX() + x, target.getMinY() + y, out);
            }
        }
        return target;
    }

    /**
     * An empty image in the destination space.
     *
     * @throws IllegalArgumentException if the destination space cannot be determined
     */
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        ColorModel cm = destCM;
        if (cm == null) {
            ColorSpace target;
            if (this.spaces.length == 0) {
                throw new IllegalArgumentException("Destination ColorSpace is undefined");
            }
            target = this.spaces[this.spaces.length - 1];
            boolean alpha = src.getColorModel().hasAlpha();
            int n = target.getNumComponents() + (alpha ? 1 : 0);
            int[] bits = new int[n];
            for (int i = 0; i < n; i++) {
                bits[i] = 8;
            }
            cm = new ComponentColorModel(target, bits, alpha, src.isAlphaPremultiplied(),
                    alpha ? java.awt.Transparency.TRANSLUCENT : java.awt.Transparency.OPAQUE,
                    DataBuffer.TYPE_BYTE);
        }
        WritableRaster wr = cm.createCompatibleWritableRaster(src.getWidth(), src.getHeight());
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    /**
     * An empty raster with as many bands as the destination space has components.
     *
     * @throws IllegalArgumentException if exactly two spaces were not declared
     */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        if (this.spaces.length != 2) {
            throw new IllegalArgumentException("Destination ColorSpace is undefined");
        }
        int n = this.spaces[1].getNumComponents();
        return Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, src.getWidth(),
                src.getHeight(), n, new java.awt.Point(src.getMinX(), src.getMinY()));
    }

    /** The same rectangle: this operation moves nothing about. */
    public final Rectangle2D getBounds2D(BufferedImage src) {
        return this.getBounds2D(src.getRaster());
    }

    /** The same rectangle. */
    public final Rectangle2D getBounds2D(Raster src) {
        return src.getBounds();
    }

    /** The same point. */
    public final Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        Point2D out = dstPt;
        if (out == null) {
            out = new java.awt.geom.Point2D.Float();
        }
        out.setLocation(srcPt.getX(), srcPt.getY());
        return out;
    }

    /** The rendering hints, or `null` if there are none. */
    public final RenderingHints getRenderingHints() {
        return this.hints;
    }
}
