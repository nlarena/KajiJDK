package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Convolution: each output pixel is a weighted sum of its neighbours, with the weights of a
 * {@link Kernel}.
 *
 * <p>It is the operation almost every effect that looks at more than one pixel comes out of. With a
 * kernel of equal values that add up to 1, blur; with one that subtracts the neighbours from the
 * centre, sharpening; with an asymmetric one, edge detection or embossing. The code is the same and
 * the table changes.
 *
 * <p>The **edge** is the real problem and that is why it is declared. The pixels of the edge do not
 * have all of their neighbours, and there are two possible answers: {@link #EDGE_ZERO_FILL} sets
 * them to zero, which leaves a dark frame; {@link #EDGE_NO_OP} copies them untouched, which leaves
 * an unfiltered frame. Neither is right —the information is not there—, and choosing which lie one
 * prefers is part of the operation.
 *
 * <p>The source and the destination **may not be the same object**, and this note used to say the
 * opposite: {@link #filter} rejects it with an {@link IllegalArgumentException}. The reason is that
 * each output depends on the neighbouring inputs, so writing over the source while reading it would
 * contaminate the following pixels.
 */
public class ConvolveOp implements BufferedImageOp, RasterOp {

    /** The pixels of the edge come out as zero. */
    public static final int EDGE_ZERO_FILL = 0;

    /** The pixels of the edge are copied unfiltered. */
    public static final int EDGE_NO_OP = 1;

    private final Kernel kernel;
    private final int edgeHint;
    private final RenderingHints hints;

    /**
     * With the kernel, the edge condition and the hints.
     *
     * @throws NullPointerException if the kernel is `null`
     */
    public ConvolveOp(Kernel kernel, int edgeCondition, RenderingHints hints) {
        this.kernel = kernel;
        this.edgeHint = edgeCondition;
        this.hints = hints;
    }

    /**
     * With the kernel and the edge in zero.
     *
     * @throws NullPointerException if the kernel is `null`
     */
    public ConvolveOp(Kernel kernel) {
        this.kernel = kernel;
        this.edgeHint = EDGE_ZERO_FILL;
        this.hints = null;
    }

    /** What is done with the pixels of the edge. */
    public int getEdgeCondition() {
        return this.edgeHint;
    }

    /** The kernel. */
    public final Kernel getKernel() {
        return (Kernel) this.kernel.clone();
    }

    /**
     * Applies the convolution to an image.
     *
     * @param dst the destination, or `null` for it to be created
     * @throws IllegalArgumentException if the source and the destination are the same object, if
     *     the source has a palette, or if the sizes do not match
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (src == dst) {
            throw new IllegalArgumentException("src image cannot be the same as the dst image");
        }
        ColorModel srcCM = src.getColorModel();
        if (srcCM instanceof IndexColorModel) {
            throw new IllegalArgumentException("ConvolveOp cannot be performed on an indexed "
                    + "image");
        }
        BufferedImage dest = dst;
        if (dest == null) {
            dest = this.createCompatibleDestImage(src, null);
        } else if (src.getWidth() != dest.getWidth()
                || src.getHeight() != dest.getHeight()) {
            throw new IllegalArgumentException("Width or height of BufferedImages do not match");
        }
        // It convolves with the alpha **premultiplied**: without premultiplying, the colour of an
        // invisible pixel would weigh the same as that of an opaque one and would bleed over its
        // neighbours.
        BufferedImage source = src;
        if (srcCM.hasAlpha() && !srcCM.isAlphaPremultiplied()) {
            ColorModel cm = srcCM;
            WritableRaster wr = cm.createCompatibleWritableRaster(src.getWidth(),
                    src.getHeight());
            source = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
            source.setData(src.getRaster());
            source.coerceData(true);
        }
        this.convolve(source.getRaster(), dest.getRaster());
        if (dest.getColorModel().hasAlpha()
                && !dest.getColorModel().isAlphaPremultiplied()) {
            dest.coerceData(true);
            dest.coerceData(false);
        }
        return dest;
    }

    /**
     * Applies the convolution to a raster.
     *
     * @param dst the destination, or `null` for it to be created
     * @throws IllegalArgumentException if the source and the destination are the same object, or if
     *     the sizes or the number of bands do not match
     */
    public final WritableRaster filter(Raster src, WritableRaster dst) {
        if (dst == src) {
            throw new IllegalArgumentException("src image cannot be the same as the dst image");
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
        this.convolve(src, dest);
        return dest;
    }

    /** The sum, band by band. */
    private void convolve(Raster src, WritableRaster dst) {
        int w = src.getWidth();
        int h = src.getHeight();
        int bands = Math.min(src.getNumBands(), dst.getNumBands());
        int kw = this.kernel.getWidth();
        int kh = this.kernel.getHeight();
        int kx = this.kernel.getXOrigin();
        int ky = this.kernel.getYOrigin();
        float[] weights = this.kernel.getKernelData(null);
        int sx = src.getMinX();
        int sy = src.getMinY();
        int dx = dst.getMinX();
        int dy = dst.getMinY();
        for (int b = 0; b < bands; b++) {
            int max = (1 << dst.getSampleModel().getSampleSize(b)) - 1;
            int[] in = src.getSamples(sx, sy, w, h, b, (int[]) null);
            int[] out = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean edge = x < kx || y < ky || x >= w - (kw - kx - 1)
                            || y >= h - (kh - ky - 1);
                    if (edge) {
                        if (this.edgeHint == EDGE_NO_OP) {
                            out[y * w + x] = in[y * w + x];
                        } else {
                            out[y * w + x] = 0;
                        }
                        continue;
                    }
                    float sum = 0.0f;
                    int k = 0;
                    for (int j = 0; j < kh; j++) {
                        int fy = y + j - ky;
                        for (int i = 0; i < kw; i++) {
                            int fx = x + i - kx;
                            sum = sum + weights[k] * in[fy * w + fx];
                            k = k + 1;
                        }
                    }
                    int v = (int) (sum + 0.5f);
                    if (v < 0) {
                        v = 0;
                    } else if (v > max) {
                        v = max;
                    }
                    out[y * w + x] = v;
                }
            }
            dst.setSamples(dx, dy, w, h, b, out);
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
                        "ConvolveOp cannot be performed on an indexed image");
            }
        }
        int w = src.getWidth();
        int h = src.getHeight();
        WritableRaster wr = cm.createCompatibleWritableRaster(w, h);
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    /** An empty raster of the same size and layout. */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        return src.createCompatibleWritableRaster();
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
