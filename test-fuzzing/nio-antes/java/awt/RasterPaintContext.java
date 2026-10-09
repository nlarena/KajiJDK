package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * The common part of all painting contexts computed point by point.
 *
 * <p>It does the work that does not change between a gradient and a texture: inverting the
 * transformation, walking the requested rectangle, taking each pixel from device coordinates to
 * user coordinates and building the raster. The only thing each paint supplies is {@link #colorAt}.
 *
 * <p>The pixel is sampled at its **centre** —hence the half pixel that is added— and not at its
 * corner. Sampling at the corner shifts the gradient half a pixel, which shows as a seam when two
 * shapes painted with the same gradient touch.
 *
 * <p>It is not public: it is a detail of how this package's paints are written.
 */
abstract class RasterPaintContext implements PaintContext {

    private final AffineTransform inverse;
    private final ColorModel model = ColorModel.getRGBdefault();

    /**
     * With the user-to-device transformation, which is inverted only once.
     *
     * @throws NoninvertibleTransformException if the transformation flattens the plane
     */
    RasterPaintContext(AffineTransform xform) throws NoninvertibleTransformException {
        this.inverse = xform.createInverse();
    }

    /** There is nothing to release: the raster is built on each request. */
    public void dispose() {
    }

    /** Always 8-bit-per-channel ARGB. */
    public ColorModel getColorModel() {
        return this.model;
    }

    /** The pixels of that device rectangle. */
    public Raster getRaster(int x, int y, int w, int h) {
        WritableRaster r = this.model.createCompatibleWritableRaster(w, h);
        double[] p = new double[2];
        int[] row = new int[w];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                p[0] = x + i + 0.5;
                p[1] = y + j + 0.5;
                this.inverse.transform(p, 0, p, 0, 1);
                row[i] = this.colorAt(p[0], p[1]);
            }
            r.setDataElements(0, j, w, 1, row);
        }
        return r;
    }

    /** The ARGB colour that belongs to that point in user coordinates. */
    abstract int colorAt(double ux, double uy);
}
