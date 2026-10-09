package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * An operation from one {@link BufferedImage} to another.
 *
 * <p>Unlike {@link RasterOp}, it sees the **colour**: it knows which space the source and
 * destination pixels are in, and can convert between them.
 */
public interface BufferedImageOp {

    /**
     * Applies the operation.
     *
     * @param dest the destination, or `null` for one to be created
     * @return the destination
     */
    BufferedImage filter(BufferedImage src, BufferedImage dest);

    /** The rectangle the result is going to take. */
    Rectangle2D getBounds2D(BufferedImage src);

    /**
     * An empty destination of the size and format that fits.
     *
     * @param destCM the colour model of the destination, or `null` to use the source's
     */
    BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM);

    /**
     * Where that point ends up.
     *
     * @param dstPt where to write the result, or `null` for one to be created
     */
    Point2D getPoint2D(Point2D srcPt, Point2D dstPt);

    /** The rendering hints, or `null` if there are none. */
    RenderingHints getRenderingHints();
}
