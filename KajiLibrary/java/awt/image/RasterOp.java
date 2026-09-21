package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * An operation from one {@link Raster} to another.
 *
 * <p>It works over **uninterpreted** pixels: it does not know what colour they are, only what
 * numbers they hold. That is the difference from {@link BufferedImageOp}, which does see the colour
 * and can therefore convert from one space to another. The same classes usually implement both
 * interfaces, and do different sums depending on which one they are called through.
 *
 * <p>{@link #getPoint2D} exists because an operation can move the pixels about: in an affine
 * transformation the destination point is not the same as the source one, and it has to be possible
 * to ask without applying the whole operation.
 */
public interface RasterOp {

    /**
     * Applies the operation.
     *
     * @param dest the destination, or `null` for one to be created
     * @return the destination
     */
    WritableRaster filter(Raster src, WritableRaster dest);

    /** The rectangle the result is going to take. */
    Rectangle2D getBounds2D(Raster src);

    /**
     * An empty destination of the size and format that fits.
     *
     * @throws IllegalArgumentException if the source does not suit this operation
     */
    WritableRaster createCompatibleDestRaster(Raster src);

    /**
     * Where that point ends up.
     *
     * @param dstPt where to write the result, or `null` for one to be created
     */
    Point2D getPoint2D(Point2D srcPt, Point2D dstPt);

    /** The rendering hints, or `null` if there are none. */
    RenderingHints getRenderingHints();
}
