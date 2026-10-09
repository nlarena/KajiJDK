package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Moves the pixels: rotates, scales, shears or translates an image with an affine transformation.
 *
 * <p>It is the only operation of the package that changes **where** each pixel is, and hence the
 * only one whose {@link #getPoint2D} does anything interesting.
 *
 * <p>The walk goes the other way round from what one would expect. Each pixel of the source is not
 * taken and its landing place computed —that would leave holes when enlarging and collisions when
 * shrinking—, but rather each pixel of the **destination** is taken and where it comes from is
 * computed, inverting the transformation. That is why the constructor demands that the
 * transformation be invertible: with no inverse there is nowhere to read from.
 *
 * <p>That source point almost never falls right on a pixel, and that is where the interpolation
 * comes in. {@link #TYPE_NEAREST_NEIGHBOR} grabs the nearest one and is fast and stepped;
 * {@link #TYPE_BILINEAR} averages the four that surround it; {@link #TYPE_BICUBIC} uses sixteen and
 * a curve that preserves the edges better, at the cost of being able to overshoot the range and
 * needing clamping.
 */
public class AffineTransformOp implements BufferedImageOp, RasterOp {

    /** The nearest pixel. */
    public static final int TYPE_NEAREST_NEIGHBOR = 1;

    /** Weighted average of the four neighbours. */
    public static final int TYPE_BILINEAR = 2;

    /** Cubic curve over sixteen neighbours. */
    public static final int TYPE_BICUBIC = 3;

    private final AffineTransform xform;
    private final int interpolationType;
    private final RenderingHints hints;

    /**
     * With the given interpolation type.
     *
     * @throws ImagingOpException if the transformation is not invertible
     * @throws IllegalArgumentException if the interpolation type is not one of the three
     */
    public AffineTransformOp(AffineTransform xform, int interpolationType) {
        this.requireInvertible(xform);
        if (interpolationType != TYPE_NEAREST_NEIGHBOR && interpolationType != TYPE_BILINEAR
                && interpolationType != TYPE_BICUBIC) {
            throw new IllegalArgumentException("Unknown interpolation type: " + interpolationType);
        }
        this.xform = (AffineTransform) xform.clone();
        this.interpolationType = interpolationType;
        this.hints = null;
    }

    /**
     * With the interpolation type taken from the hints.
     *
     * <p>With no hints, or with no interpolation hint, the nearest neighbour is used: it is the
     * fastest and what fits when nobody asked for quality. There is one exception, and this note
     * used to leave it out: with no interpolation hint but with {@code KEY_RENDERING} set to {@code
     * VALUE_RENDER_QUALITY}, bilinear is used.
     *
     * @throws ImagingOpException if the transformation is not invertible
     */
    public AffineTransformOp(AffineTransform xform, RenderingHints hints) {
        this.requireInvertible(xform);
        this.xform = (AffineTransform) xform.clone();
        this.hints = hints;
        int type = TYPE_NEAREST_NEIGHBOR;
        if (hints != null) {
            Object value = hints.get(RenderingHints.KEY_INTERPOLATION);
            if (value == null) {
                Object quality = hints.get(RenderingHints.KEY_RENDERING);
                if (quality == RenderingHints.VALUE_RENDER_QUALITY) {
                    type = TYPE_BILINEAR;
                }
            } else if (value == RenderingHints.VALUE_INTERPOLATION_BILINEAR) {
                type = TYPE_BILINEAR;
            } else if (value == RenderingHints.VALUE_INTERPOLATION_BICUBIC) {
                type = TYPE_BICUBIC;
            }
        }
        this.interpolationType = type;
    }

    /**
     * Checks that the transformation can be inverted.
     *
     * @throws ImagingOpException if the determinant is zero or nearly so
     */
    private void requireInvertible(AffineTransform xform) {
        double det = xform.getDeterminant();
        if (Math.abs(det) <= Double.MIN_VALUE) {
            throw new ImagingOpException("Unable to invert transform " + xform);
        }
    }

    /** The interpolation type. */
    public final int getInterpolationType() {
        return this.interpolationType;
    }

    /** The transformation. */
    public final AffineTransform getTransform() {
        return (AffineTransform) this.xform.clone();
    }

    /**
     * Applies the transformation to an image.
     *
     * @param dst the destination, or `null` for it to be created
     * @throws IllegalArgumentException if the source and the destination are the same object
     * @throws ImagingOpException if the transformation cannot be inverted
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (src == null) {
            throw new NullPointerException("src image is null");
        }
        if (src == dst) {
            throw new IllegalArgumentException("src image cannot be the same as the dst image");
        }
        BufferedImage dest = dst;
        if (dest == null) {
            dest = this.createCompatibleDestImage(src, src.getColorModel());
        }
        // It is transformed in ARGB and not in the format of the image: interpolating demands
        // averaging colours, and averaging palette indices or bit fields means nothing.
        int sw = src.getWidth();
        int sh = src.getHeight();
        int dw = dest.getWidth();
        int dh = dest.getHeight();
        int[] source = new int[sw * sh];
        src.getRGB(0, 0, sw, sh, source, 0, sw);
        int[] out = new int[dw * dh];
        AffineTransform inv;
        try {
            inv = this.xform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new ImagingOpException("Unable to invert transform " + this.xform);
        }
        double[] pt = new double[2];
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                pt[0] = x + 0.5;
                pt[1] = y + 0.5;
                inv.transform(pt, 0, pt, 0, 1);
                out[y * dw + x] = this.sample(source, sw, sh, pt[0] - 0.5,
                        pt[1] - 0.5);
            }
        }
        dest.setRGB(0, 0, dw, dh, out, 0, dw);
        return dest;
    }

    /** The ARGB colour that corresponds to that continuous coordinate of the source. */
    private int sample(int[] src, int w, int h, double x, double y) {
        if (this.interpolationType == TYPE_NEAREST_NEIGHBOR) {
            int ix = (int) Math.floor(x + 0.5);
            int iy = (int) Math.floor(y + 0.5);
            return pixelAt(src, w, h, ix, iy);
        }
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        if (this.interpolationType == TYPE_BILINEAR) {
            int[] channels = new int[4];
            for (int c = 0; c < 4; c++) {
                double a = channel(pixelAt(src, w, h, x0, y0), c);
                double b = channel(pixelAt(src, w, h, x0 + 1, y0), c);
                double d = channel(pixelAt(src, w, h, x0, y0 + 1), c);
                double e = channel(pixelAt(src, w, h, x0 + 1, y0 + 1), c);
                double top = a + (b - a) * fx;
                double bottom = d + (e - d) * fx;
                channels[c] = clamp(top + (bottom - top) * fy);
            }
            return packArgb(channels);
        }
        int[] channels = new int[4];
        for (int c = 0; c < 4; c++) {
            double[] rows = new double[4];
            for (int j = 0; j < 4; j++) {
                double[] v = new double[4];
                for (int i = 0; i < 4; i++) {
                    v[i] = channel(pixelAt(src, w, h, x0 - 1 + i, y0 - 1 + j), c);
                }
                rows[j] = cubic(v, fx);
            }
            channels[c] = clamp(cubic(rows, fy));
        }
        return packArgb(channels);
    }

    /**
     * The Catmull-Rom cubic curve over four values.
     *
     * <p>It passes exactly through the two in the middle and uses the ones at the sides only for
     * the slope, which is what gives it cleaner edges than the bilinear one — and also what lets it
     * overshoot the range and need clamping.
     */
    private static double cubic(double[] v, double t) {
        double a = v[3] - v[2] - v[0] + v[1];
        double b = v[0] - v[1] - a;
        double c = v[2] - v[0];
        double d = v[1];
        return a * t * t * t + b * t * t + c * t + d;
    }

    /** The pixel of that position, or transparent if it falls outside. */
    private static int pixelAt(int[] src, int w, int h, int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) {
            return 0;
        }
        return src[y * w + x];
    }

    /** Channel `c` of an ARGB: 0 alpha, 1 red, 2 green, 3 blue. */
    private static int channel(int argb, int c) {
        return (argb >> (24 - c * 8)) & 0xFF;
    }

    /** A value brought to 0..255. */
    private static int clamp(double v) {
        int i = (int) (v + 0.5);
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }

    /** The four channels back in one ARGB. */
    private static int packArgb(int[] channels) {
        return (channels[0] << 24) | (channels[1] << 16) | (channels[2] << 8) | channels[3];
    }

    /**
     * Applies the transformation to a raster.
     *
     * @param dst the destination, or `null` for it to be created
     * @throws IllegalArgumentException if the source and the destination are the same object, or if
     *     they do not have the same number of bands
     * @throws ImagingOpException if the transformation cannot be inverted
     */
    public final WritableRaster filter(Raster src, WritableRaster dst) {
        if (src == null) {
            throw new NullPointerException("src image is null");
        }
        if (src == dst) {
            throw new IllegalArgumentException("src image cannot be the same as the dst image");
        }
        WritableRaster dest = dst;
        if (dest == null) {
            dest = this.createCompatibleDestRaster(src);
        } else if (src.getNumBands() != dest.getNumBands()) {
            throw new IllegalArgumentException("Number of src bands (" + src.getNumBands()
                    + ") does not match number of dst bands (" + dest.getNumBands() + ")");
        }
        AffineTransform inv;
        try {
            inv = this.xform.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new ImagingOpException("Unable to invert transform " + this.xform);
        }
        int sw = src.getWidth();
        int sh = src.getHeight();
        int sx = src.getMinX();
        int sy = src.getMinY();
        int dw = dest.getWidth();
        int dh = dest.getHeight();
        int bands = src.getNumBands();
        int[] pixel = new int[bands];
        double[] pt = new double[2];
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                pt[0] = x + 0.5;
                pt[1] = y + 0.5;
                inv.transform(pt, 0, pt, 0, 1);
                int ix = (int) Math.floor(pt[0]);
                int iy = (int) Math.floor(pt[1]);
                if (ix < 0 || iy < 0 || ix >= sw || iy >= sh) {
                    for (int b = 0; b < bands; b++) {
                        pixel[b] = 0;
                    }
                } else {
                    pixel = src.getPixel(sx + ix, sy + iy, pixel);
                }
                dest.setPixel(dest.getMinX() + x, dest.getMinY() + y, pixel);
            }
        }
        return dest;
    }

    /** The rectangle the result is going to take. */
    public final Rectangle2D getBounds2D(BufferedImage src) {
        return this.getBounds2D(src.getRaster());
    }

    /** The rectangle the result is going to take. */
    public final Rectangle2D getBounds2D(Raster src) {
        int w = src.getWidth();
        int h = src.getHeight();
        double[] corners = new double[8];
        corners[0] = 0;
        corners[1] = 0;
        corners[2] = w;
        corners[3] = 0;
        corners[4] = w;
        corners[5] = h;
        corners[6] = 0;
        corners[7] = h;
        this.xform.transform(corners, 0, corners, 0, 4);
        double minX = corners[0];
        double maxX = corners[0];
        double minY = corners[1];
        double maxY = corners[1];
        for (int i = 2; i < 8; i = i + 2) {
            minX = Math.min(minX, corners[i]);
            maxX = Math.max(maxX, corners[i]);
            minY = Math.min(minY, corners[i + 1]);
            maxY = Math.max(maxY, corners[i + 1]);
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * An empty image of the size the result is going to take.
     *
     * <p>The size comes from {@link #getBounds2D} and not from the source: rotating a square image
     * needs a bigger destination.
     */
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        ColorModel cm = destCM;
        Rectangle2D r = this.getBounds2D(src.getRaster());
        int w = (int) Math.ceil(r.getWidth());
        int h = (int) Math.ceil(r.getHeight());
        if (cm == null) {
            cm = src.getColorModel();
            if (cm instanceof IndexColorModel && src.getType() != BufferedImage.TYPE_BYTE_BINARY) {
                cm = ColorModel.getRGBdefault();
            }
        }
        WritableRaster wr = cm.createCompatibleWritableRaster(w, h);
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    /** An empty raster of the size the result is going to take. */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        Rectangle2D r = this.getBounds2D(src);
        return src.createCompatibleWritableRaster((int) r.getX(), (int) r.getY(),
                (int) Math.ceil(r.getWidth()), (int) Math.ceil(r.getHeight()));
    }

    /** Where that point ends up. */
    public final Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return this.xform.transform(srcPt, dstPt);
    }

    /** The rendering hints, or `null` if there are none. */
    public final RenderingHints getRenderingHints() {
        return this.hints;
    }
}
