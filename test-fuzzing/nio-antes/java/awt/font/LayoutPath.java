package java.awt.font;

import java.awt.geom.Point2D;

/**
 * A path the text rests on, which need not be a straight line.
 *
 * <p>It converts between two coordinate systems: the **screen's**, where the text is already drawn,
 * and the **line's**, where a coordinate is "so much advance along the line, so much away from the
 * baseline". On a straight line the conversion is trivial; on a circle or a curve it is not, and that
 * is the point of the class.
 *
 * <p>{@link #pointToPath} also has to say which **side** the point fell on, because a curved path may
 * pass close to itself and the distance alone is not enough to know which part of the line a click
 * belongs to.
 */
public abstract class LayoutPath {

    /** For the subclasses. */
    protected LayoutPath() {
    }

    /**
     * From screen coordinates to line coordinates.
     *
     * @param point the screen point
     * @param location where to write the result
     * @return `true` if the point falls on the path's left side, looking in its direction
     */
    public abstract boolean pointToPath(Point2D point, Point2D location);

    /**
     * From line coordinates to screen coordinates.
     *
     * @param location the point on the line
     * @param preceding whether an ambiguity has to be settled by taking the preceding stretch
     * @param point where to write the result
     */
    public abstract void pathToPoint(Point2D location, boolean preceding, Point2D point);
}
