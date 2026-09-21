package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Replaces each band value by the one a table says.
 *
 * <p>It is the most general of the operations that work pixel by pixel without looking at the
 * neighbours: the table can describe any function from one value to another. Inverting an image,
 * applying a gamma curve, posterising, thresholding: they are all the same operation with a
 * different table.
 *
 * <p>And it is fast for exactly that reason: there is no sum to do per pixel, only one array
 * access. The curve is computed once when the table is built.
 *
 * <p>As in {@link RescaleOp}, over a {@link BufferedImage} the **alpha is not touched** unless the
 * table has as many arrays as the model has components counting the alpha.
 */
public class LookupOp implements BufferedImageOp, RasterOp {

    private final LookupTable ltable;
    private final int numComponents;
    private final RenderingHints hints;

    /**
     * With the given table.
     *
     * @throws NullPointerException if the table is `null`
     */
    public LookupOp(LookupTable lookup, RenderingHints hints) {
        this.ltable = lookup;
        this.numComponents = this.ltable.getNumComponents();
        this.hints = hints;
    }

    /** The table. */
    public final LookupTable getTable() {
        return this.ltable;
    }

    /**
     * Applies the operation to an image.
     *
     * @param dst the destination, or `null` for it to be created
     * @throws IllegalArgumentException if the source has a palette, if the sizes do not match, or
     *     if the table has neither 1 array nor as many as there are components
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dst) {
        ColorModel srcCM = src.getColorModel();
        if (srcCM instanceof IndexColorModel) {
            throw new IllegalArgumentException("LookupOp cannot be "
                    + "performed on an indexed image");
        }
        int numColour = srcCM.getNumColorComponents();
        int numAll = srcCM.getNumComponents();
        if (this.numComponents != 1 && this.numComponents != numColour
                && this.numComponents != numAll) {
            throw new IllegalArgumentException("Number of arrays in the  lookup table ("
                    + this.numComponents + ") is not compatible with the  src image: " + src);
        }
        BufferedImage dest = dst;
        if (dest == null) {
            dest = this.createCompatibleDestImage(src, null);
        } else if (src.getWidth() != dest.getWidth()
                || src.getHeight() != dest.getHeight()) {
            throw new IllegalArgumentException("Width or height of BufferedImages do not match");
        }
        BufferedImage source = src;
        if (srcCM.isAlphaPremultiplied()) {
            ColorModel cm = srcCM;
            WritableRaster wr = cm.createCompatibleWritableRaster(src.getWidth(),
                    src.getHeight());
            source = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
            source.setData(src.getRaster());
            source.coerceData(false);
        }
        boolean touchAlpha = this.numComponents == numAll && srcCM.hasAlpha();
        int bands = touchAlpha ? numAll : numColour;
        this.applyTable(source.getRaster(), dest.getRaster(), bands);
        if (!touchAlpha && srcCM.hasAlpha() && dest.getColorModel().hasAlpha()) {
            int w = src.getWidth();
            int[] row = new int[w];
            for (int y = 0; y < src.getHeight(); y++) {
                row = source.getRaster().getSamples(0, y, w, 1, numColour, row);
                dest.getRaster().setSamples(0, y, w, 1, numColour, row);
            }
        }
        if (dest.getColorModel().isAlphaPremultiplied()) {
            dest.coerceData(true);
        }
        return dest;
    }

    /**
     * Applies the operation to a raster.
     *
     * @param dst the destination, or `null` for it to be created
     * @throws IllegalArgumentException if the sizes or the number of bands do not match, or if the
     *     table has neither 1 array nor as many as there are bands
     */
    public final WritableRaster filter(Raster src, WritableRaster dst) {
        int numBands = src.getNumBands();
        if (this.numComponents != 1 && this.numComponents != numBands) {
            throw new IllegalArgumentException("Number of arrays in the  lookup table ("
                    + this.numComponents + ") is not compatible with the  src Raster: " + src);
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
                throw new IllegalArgumentException("Width or height of Rasters do not match");
            }
        }
        this.applyTable(src, dest, numBands);
        return dest;
    }

    /** Passes the first `bands` bands through the table, pixel by pixel. */
    private void applyTable(Raster src, WritableRaster dst, int bands) {
        int w = src.getWidth();
        int h = src.getHeight();
        int sx = src.getMinX();
        int sy = src.getMinY();
        int dx = dst.getMinX();
        int dy = dst.getMinY();
        int[] pixel = new int[Math.max(src.getNumBands(), this.numComponents)];
        int[] out = new int[pixel.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixel = src.getPixel(sx + x, sy + y, pixel);
                // Only the bands that correspond are passed through the table; the rest is copied.
                int[] partial = new int[bands];
                System.arraycopy(pixel, 0, partial, 0, bands);
                int[] converted = this.ltable.lookupPixel(partial, null);
                System.arraycopy(pixel, 0, out, 0, pixel.length);
                System.arraycopy(converted, 0, out, 0, bands);
                dst.setPixel(dx + x, dy + y, out);
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
                        "LookupOp cannot be performed on an indexed image");
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
