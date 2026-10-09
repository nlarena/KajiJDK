package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Combines the bands of a raster with a matrix: each output band is a weighted sum of the input
 * ones.
 *
 * <p>It is what makes it possible to go from RGB to grey with the right weights, to swap channels,
 * to separate luminance and chrominance, or any linear mix of bands in a single pass.
 *
 * <p>The matrix has one row per output band. If it also has **one column too many**, that last one
 * is multiplied by an implicit 1 and works as a constant term: it is the homogeneous coordinates
 * trick, and it allows writing a shift —brightness— inside the same matrix.
 *
 * <p>It is the only one of the operations of this package that does **not** work over images.
 * Mixing bands is an operation over uninterpreted numbers: band 0 of one image is red and that of
 * another is cyan, and a matrix that combined them would not know what it is producing. That is why
 * it implements {@link RasterOp} and not {@link BufferedImageOp}.
 */
public class BandCombineOp implements RasterOp {

    private final float[][] matrix;
    private final int nrows;
    private final int ncols;
    private final RenderingHints hints;

    /**
     * With the given matrix.
     *
     * @throws NullPointerException if the matrix is `null`
     */
    public BandCombineOp(float[][] matrix, RenderingHints hints) {
        this.nrows = matrix.length;
        this.ncols = matrix[0].length;
        this.matrix = new float[this.nrows][];
        for (int i = 0; i < this.nrows; i++) {
            this.matrix[i] = new float[this.ncols];
            System.arraycopy(matrix[i], 0, this.matrix[i], 0,
                    Math.min(this.ncols, matrix[i].length));
        }
        this.hints = hints;
    }

    /** A copy of the matrix. */
    public final float[][] getMatrix() {
        float[][] out = new float[this.nrows][];
        for (int i = 0; i < this.nrows; i++) {
            out[i] = new float[this.ncols];
            System.arraycopy(this.matrix[i], 0, out[i], 0, this.ncols);
        }
        return out;
    }

    /**
     * Applies the matrix.
     *
     * @param dst the destination, or `null` for it to be created
     * @throws IllegalArgumentException if the matrix does not have as many columns as there are
     *     input bands —or one more—, or if the destination does not have as many bands as the
     *     matrix has rows
     */
    public WritableRaster filter(Raster src, WritableRaster dst) {
        int nBands = src.getNumBands();
        if (this.ncols != nBands && this.ncols != nBands + 1) {
            throw new IllegalArgumentException("Number of columns in the  matrix (" + this.ncols
                    + ") must be equal to the number of bands ([+1]) in src (" + nBands + ").");
        }
        WritableRaster dest = dst;
        if (dest == null) {
            dest = this.createCompatibleDestRaster(src);
        } else if (this.nrows != dest.getNumBands()) {
            throw new IllegalArgumentException("Number of rows in the  matrix (" + this.nrows
                    + ") must be equal to the number of bands ([+1]) in dst (" + nBands + ").");
        }
        int w = src.getWidth();
        int h = src.getHeight();
        int sx = src.getMinX();
        int sy = src.getMinY();
        int dx = dest.getMinX();
        int dy = dest.getMinY();
        // The extra column is multiplied by an implicit 1: it is the constant term.
        boolean constant = this.ncols == nBands + 1;
        int[] in = new int[nBands];
        int[] out = new int[this.nrows];
        int[] maxima = new int[this.nrows];
        for (int b = 0; b < this.nrows; b++) {
            maxima[b] = (1 << dest.getSampleModel().getSampleSize(b)) - 1;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                in = src.getPixel(sx + x, sy + y, in);
                for (int b = 0; b < this.nrows; b++) {
                    float sum = 0.0f;
                    for (int c = 0; c < nBands; c++) {
                        sum = sum + this.matrix[b][c] * in[c];
                    }
                    if (constant) {
                        sum = sum + this.matrix[b][nBands];
                    }
                    int v = (int) (sum + 0.5f);
                    if (v < 0) {
                        v = 0;
                    } else if (v > maxima[b]) {
                        v = maxima[b];
                    }
                    out[b] = v;
                }
                dest.setPixel(dx + x, dy + y, out);
            }
        }
        return dest;
    }

    /**
     * An empty raster with as many bands as the matrix has rows.
     *
     * @throws IllegalArgumentException if the matrix does not fit the bands of the source
     */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        int nBands = src.getNumBands();
        if (this.ncols != nBands && this.ncols != nBands + 1) {
            throw new IllegalArgumentException("Number of columns in the  matrix (" + this.ncols
                    + ") must be equal to the number of bands ([+1]) in src (" + nBands + ").");
        }
        if (this.nrows == nBands) {
            return src.createCompatibleWritableRaster();
        }
        SampleModel sm = src.getSampleModel();
        int[] bands = new int[this.nrows];
        for (int i = 0; i < this.nrows; i++) {
            // A destination with more bands than the source repeats the last layout: there is
            // nowhere to take the one of a band that does not exist in the source from.
            bands[i] = Math.min(i, sm.getNumBands() - 1);
        }
        SampleModel nsm = sm.createSubsetSampleModel(bands);
        return Raster.createWritableRaster(nsm, nsm.createDataBuffer(),
                new java.awt.Point(src.getMinX(), src.getMinY()));
    }

    /** The same rectangle: this operation moves nothing about. */
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
