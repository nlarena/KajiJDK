package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Multiplies and adds: `dest = source * scale + offset`, band by band.
 *
 * <p>It is the brightness and contrast operation. The scale opens or closes the range —contrast—
 * and the offset moves it whole —brightness—. With a single constant it is applied to every band;
 * with several, one per band.
 *
 * <p>It works over the values **just as they are stored**, not over 0..1, so a scale of 2 over an
 * eight-bit band takes 100 to 200 and 200 to 255 clamped. That clamping is the part that shows:
 * raising the brightness squashes the highlights against the ceiling and that information does not
 * come back.
 *
 * <p>Over a {@link BufferedImage} the **alpha is not touched** unless as many constants are given
 * as the model has components, counting the alpha. It is the reasonable thing: raising the
 * brightness of an image should not make it opaque. Over a {@link Raster} there is no colour model
 * to consult and every band is the same, so all of them are scaled.
 */
public class RescaleOp implements BufferedImageOp, RasterOp {

    private final float[] scaleFactors;
    private final float[] offsets;
    private final int length;
    private final RenderingHints hints;

    /**
     * With one constant per band.
     *
     * @throws IllegalArgumentException if the two arrays do not measure the same
     */
    public RescaleOp(float[] scaleFactors, float[] offsets, RenderingHints hints) {
        this.length = scaleFactors.length;
        if (this.length != offsets.length) {
            throw new IllegalArgumentException("Number of scaling factors does not equal the "
                    + "number of offsets");
        }
        this.scaleFactors = new float[this.length];
        this.offsets = new float[this.length];
        for (int i = 0; i < this.length; i++) {
            this.scaleFactors[i] = scaleFactors[i];
            this.offsets[i] = offsets[i];
        }
        this.hints = hints;
    }

    /** With the same constant for every band. */
    public RescaleOp(float scaleFactor, float offset, RenderingHints hints) {
        this.length = 1;
        this.scaleFactors = new float[1];
        this.offsets = new float[1];
        this.scaleFactors[0] = scaleFactor;
        this.offsets[0] = offset;
        this.hints = hints;
    }

    /**
     * The scales.
     *
     * @param scaleFactors where to write them, or `null` for the array to be created
     * @throws IllegalArgumentException if the given array is shorter
     */
    public final float[] getScaleFactors(float[] scaleFactors) {
        float[] out = scaleFactors;
        if (out == null) {
            out = new float[this.length];
        }
        System.arraycopy(this.scaleFactors, 0, out, 0, Math.min(this.length, out.length));
        return out;
    }

    /**
     * The offsets.
     *
     * @param offsets where to write them, or `null` for the array to be created
     */
    public final float[] getOffsets(float[] offsets) {
        float[] out = offsets;
        if (out == null) {
            out = new float[this.length];
        }
        System.arraycopy(this.offsets, 0, out, 0, Math.min(this.length, out.length));
        return out;
    }

    /** How many constants there are. */
    public final int getNumFactors() {
        return this.length;
    }

    /** The constant that falls to that band. */
    private float scaleOf(int b) {
        return this.length == 1 ? this.scaleFactors[0] : this.scaleFactors[b];
    }

    /** The offset that falls to that band. */
    private float offsetOf(int b) {
        return this.length == 1 ? this.offsets[0] : this.offsets[b];
    }

    /**
     * Applies the operation to an image.
     *
     * <p>With premultiplied alpha the source is first taken to non-premultiplied: scaling a colour
     * that is already multiplied by its alpha would give a result that depends on the transparency,
     * which is not what is asked for.
     *
     * @param dst the destination, or `null` for it to be created
     * @throws IllegalArgumentException if the source has a palette, if the sizes do not match, or
     *     if the number of constants is neither 1 nor the number of components
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dst) {
        ColorModel srcCM = src.getColorModel();
        if (srcCM instanceof IndexColorModel) {
            throw new IllegalArgumentException(
                    "Rescaling cannot be performed on an indexed image");
        }
        int numColour = srcCM.getNumColorComponents();
        int numAll = srcCM.getNumComponents();
        if (this.length != 1 && this.length != numColour && this.length != numAll) {
            throw new IllegalArgumentException("Number of scaling constants does not equal the "
                    + "number of of color or color/alpha components");
        }
        BufferedImage dest = dst;
        if (dest == null) {
            dest = this.createCompatibleDestImage(src, null);
        } else if (src.getWidth() != dest.getWidth()
                || src.getHeight() != dest.getHeight()) {
            throw new IllegalArgumentException("Width or height of BufferedImages do not match");
        }
        // It always works without premultiplying and premultiplies again at the end if the
        // destination asks for it: they are two conversions too many in the worst case, and the
        // only way for the sum to mean the same in the four combinations of source and destination.
        BufferedImage source = src;
        if (srcCM.isAlphaPremultiplied()) {
            source = this.copyUnpremultiplied(src);
        }
        boolean touchAlpha = this.length == numAll && srcCM.hasAlpha();
        int bands = touchAlpha ? numAll : numColour;
        this.rescaleRaster(source.getRaster(), dest.getRaster(), bands);
        if (!touchAlpha && srcCM.hasAlpha() && dest.getColorModel().hasAlpha()) {
            this.copyAlpha(source.getRaster(), dest.getRaster(), numColour);
        }
        if (dest.getColorModel().isAlphaPremultiplied()) {
            dest.coerceData(true);
        }
        return dest;
    }

    /** A copy of the image with the colour not premultiplied. */
    private BufferedImage copyUnpremultiplied(BufferedImage src) {
        ColorModel cm = src.getColorModel();
        WritableRaster wr = cm.createCompatibleWritableRaster(src.getWidth(), src.getHeight());
        BufferedImage copy = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
        copy.setData(src.getRaster());
        copy.coerceData(false);
        return copy;
    }

    /** Copies the alpha band without touching it. */
    private void copyAlpha(Raster src, WritableRaster dst, int aIdx) {
        int w = Math.min(src.getWidth(), dst.getWidth());
        int h = Math.min(src.getHeight(), dst.getHeight());
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            row = src.getSamples(src.getMinX(), src.getMinY() + y, w, 1, aIdx, row);
            dst.setSamples(dst.getMinX(), dst.getMinY() + y, w, 1, aIdx, row);
        }
    }

    /**
     * Applies the operation to a raster.
     *
     * @param dst the destination, or `null` for it to be created
     * @throws IllegalArgumentException if the sizes or the number of bands do not match, or if the
     *     number of constants is neither 1 nor the number of bands
     */
    public final WritableRaster filter(Raster src, WritableRaster dst) {
        int numBands = src.getNumBands();
        if (this.length != 1 && this.length != numBands) {
            throw new IllegalArgumentException("Number of rasterBands (" + numBands
                    + ") does not match number of scale factors (" + this.length + ")");
        }
        WritableRaster dest = dst;
        if (dest == null) {
            dest = this.createCompatibleDestRaster(src);
        } else {
            if (src.getNumBands() != dest.getNumBands()) {
                throw new IllegalArgumentException("Number of src bands (" + src.getNumBands()
                        + ") does not match number of dst bands (" + dest.getNumBands() + ")");
            }
            if (src.getWidth() != dest.getWidth() || src.getHeight() != dest.getHeight()) {
                throw new IllegalArgumentException(
                        "Width or height of Rasters do not match");
            }
        }
        this.rescaleRaster(src, dest, numBands);
        return dest;
    }

    /** The largest value that band of the raster admits. */
    private static int maxOfBand(Raster r, int b) {
        return (1 << r.getSampleModel().getSampleSize(b)) - 1;
    }

    /** Applies the sum to the first `bands` bands. */
    private void rescaleRaster(Raster src, WritableRaster dst, int bands) {
        int w = src.getWidth();
        int h = src.getHeight();
        int sx = src.getMinX();
        int sy = src.getMinY();
        int dx = dst.getMinX();
        int dy = dst.getMinY();
        int type = src.getSampleModel().getDataType();
        boolean floating = type == DataBuffer.TYPE_FLOAT || type == DataBuffer.TYPE_DOUBLE;
        if (floating) {
            double[] row = new double[w];
            for (int b = 0; b < bands; b++) {
                double sc = this.scaleOf(b);
                double off = this.offsetOf(b);
                for (int y = 0; y < h; y++) {
                    row = src.getSamples(sx, sy + y, w, 1, b, row);
                    for (int i = 0; i < w; i++) {
                        row[i] = row[i] * sc + off;
                    }
                    dst.setSamples(dx, dy + y, w, 1, b, row);
                }
            }
            return;
        }
        int[] row = new int[w];
        for (int b = 0; b < bands; b++) {
            float sc = this.scaleOf(b);
            float off = this.offsetOf(b);
            int max = maxOfBand(dst, b);
            for (int y = 0; y < h; y++) {
                row = src.getSamples(sx, sy + y, w, 1, b, row);
                for (int i = 0; i < w; i++) {
                    int v = (int) (row[i] * sc + off + 0.5f);
                    if (v < 0) {
                        v = 0;
                    } else if (v > max) {
                        v = max;
                    }
                    row[i] = v;
                }
                dst.setSamples(dx, dy + y, w, 1, b, row);
            }
        }
    }

    /**
     * An empty image of the size and format that fits.
     *
     * @throws IllegalArgumentException if the source has a palette and no other colour model is
     *     given
     */
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        ColorModel cm = destCM;
        if (cm == null) {
            cm = src.getColorModel();
            if (cm instanceof IndexColorModel) {
                throw new IllegalArgumentException(
                        "Rescaling cannot be performed on an indexed image");
            }
        }
        int w = src.getWidth();
        int h = src.getHeight();
        WritableRaster wr = cm.createCompatibleWritableRaster(w, h);
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    /** An empty raster of the same size and layout. */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        return src.createCompatibleWritableRaster(src.getWidth(), src.getHeight());
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
