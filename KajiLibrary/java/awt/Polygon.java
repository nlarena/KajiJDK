package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A closed polygon with integer vertices.
 *
 * <p>The three fields that define the figure --{@code npoints}, {@code xpoints}, {@code ypoints}--
 * are public and mutable, which is a decision from 1.0 one has to live with. The consequence is
 * that the bounding box cannot be cached blindly: whoever touches the arrays from outside has to
 * call {@code invalidate()}, and that is why that method exists.
 *
 * <p>The arrays may be longer than {@code npoints}. {@code addPoint} doubles them when they fill up
 * --amortised-- so building a polygon point by point is not quadratic.
 *
 * <h2>How it is decided whether a point is inside</h2>
 *
 * <p>Even-odd rule: the crossings of a horizontal ray with the sides are counted and the parity is
 * looked at. The visible consequence is that in a self-intersecting polygon the "inside"
 * alternates, and that the top and left edges count as inside but the bottom and right ones do not.
 * That half-open behaviour is not an oversight: it is what makes two polygons placed side by side
 * share no pixel and not step on the edge when both are filled.
 *
 * <p>The tests against a rectangle are delegated to {@code Path2D} with {@code WIND_EVEN_ODD},
 * which is the same crossing machinery the rest of {@code java.awt.geom} uses. Writing a crossing
 * counter of our own for this would be repeating --and being able to get wrong-- an algorithm that
 * is already there and already tested.
 */
public class Polygon implements Shape, java.io.Serializable {

    private static final long serialVersionUID = -6460061437900069969L;

    private static final int MIN_LENGTH = 4;

    public int npoints;

    public int[] xpoints;

    public int[] ypoints;

    /** The cached bounding box, or null if it has to be worked out again. */
    protected Rectangle bounds;

    public Polygon() {
        xpoints = new int[MIN_LENGTH];
        ypoints = new int[MIN_LENGTH];
    }

    public Polygon(int[] xpoints, int[] ypoints, int npoints) {
        // The overflow is checked first and the negative count second: a negative is never greater
        // than a length, so it falls through to the check whose message describes the cause.
        if (npoints > xpoints.length || npoints > ypoints.length) {
            throw new IndexOutOfBoundsException(
                    "npoints > xpoints.length || npoints > ypoints.length");
        }
        if (npoints < 0) {
            throw new NegativeArraySizeException("npoints < 0");
        }
        this.npoints = npoints;
        this.xpoints = java.util.Arrays.copyOf(xpoints, npoints);
        this.ypoints = java.util.Arrays.copyOf(ypoints, npoints);
    }

    public void reset() {
        npoints = 0;
        bounds = null;
    }

    /**
     * It has to be called if the public arrays were touched: the cache does not find out by itself.
     */
    public void invalidate() {
        bounds = null;
    }

    public void translate(int deltaX, int deltaY) {
        for (int i = 0; i < npoints; i++) {
            xpoints[i] += deltaX;
            ypoints[i] += deltaY;
        }
        if (bounds != null) {
            // Translating the box is exact and saves walking the points a second time.
            bounds.translate(deltaX, deltaY);
        }
    }

    private void calculateBounds(int[] xpoints, int[] ypoints, int npoints) {
        int boundsMinX = Integer.MAX_VALUE;
        int boundsMinY = Integer.MAX_VALUE;
        int boundsMaxX = Integer.MIN_VALUE;
        int boundsMaxY = Integer.MIN_VALUE;

        for (int i = 0; i < npoints; i++) {
            int x = xpoints[i];
            boundsMinX = Math.min(boundsMinX, x);
            boundsMaxX = Math.max(boundsMaxX, x);
            int y = ypoints[i];
            boundsMinY = Math.min(boundsMinY, y);
            boundsMaxY = Math.max(boundsMaxY, y);
        }
        bounds = new Rectangle(boundsMinX, boundsMinY,
                boundsMaxX - boundsMinX, boundsMaxY - boundsMinY);
    }

    private void updateBounds(int x, int y) {
        if (x < bounds.x) {
            bounds.width = bounds.width + (bounds.x - x);
            bounds.x = x;
        } else {
            bounds.width = Math.max(bounds.width, x - bounds.x);
        }

        if (y < bounds.y) {
            bounds.height = bounds.height + (bounds.y - y);
            bounds.y = y;
        } else {
            bounds.height = Math.max(bounds.height, y - bounds.y);
        }
    }

    public void addPoint(int x, int y) {
        if (npoints >= xpoints.length || npoints >= ypoints.length) {
            int newLength = npoints * 2;
            if (newLength < MIN_LENGTH) {
                newLength = MIN_LENGTH;
            } else if ((newLength & (newLength - 1)) != 0) {
                newLength = Integer.highestOneBit(newLength);
            }
            xpoints = java.util.Arrays.copyOf(xpoints, newLength);
            ypoints = java.util.Arrays.copyOf(ypoints, newLength);
        }
        xpoints[npoints] = x;
        ypoints[npoints] = y;
        npoints++;
        if (bounds != null) {
            updateBounds(x, y);
        }
    }

    public Rectangle getBounds() {
        return getBoundingBox();
    }

    /**
     * The 1.0 name. In the JDK it is the one that does the work and {@code getBounds()} delegates.
     */
    public Rectangle getBoundingBox() {
        if (npoints == 0) {
            return new Rectangle();
        }
        if (bounds == null) {
            calculateBounds(xpoints, ypoints, npoints);
        }
        return bounds.getBounds();
    }

    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }

    public boolean contains(int x, int y) {
        return contains((double) x, (double) y);
    }

    /** The 1.0 name. */
    public boolean inside(int x, int y) {
        return contains((double) x, (double) y);
    }

    public Rectangle2D getBounds2D() {
        return getBounds();
    }

    /**
     * Counts the crossings with the ray that leaves the point to the left and looks at the parity.
     *
     * <p>The horizontal sides are skipped: they contribute no crossing and besides, dividing by
     * their height would be dividing by zero. The other early discards --by box, by a side wholly
     * to the left or to the right-- are there so that the common case never reaches the division.
     */
    public boolean contains(double x, double y) {
        if (npoints <= 2 || !getBoundingBox().contains(x, y)) {
            return false;
        }
        int hits = 0;

        int lastx = xpoints[npoints - 1];
        int lasty = ypoints[npoints - 1];
        int curx;
        int cury;

        for (int i = 0; i < npoints; lastx = curx, lasty = cury, i++) {
            curx = xpoints[i];
            cury = ypoints[i];

            if (cury == lasty) {
                continue;
            }

            int leftx;
            if (curx < lastx) {
                if (x >= lastx) {
                    continue;
                }
                leftx = curx;
            } else {
                if (x >= curx) {
                    continue;
                }
                leftx = lastx;
            }

            double test1;
            double test2;
            if (cury < lasty) {
                if (y < cury || y >= lasty) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - curx;
                test2 = y - cury;
            } else {
                if (y < lasty || y >= cury) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - lastx;
                test2 = y - lasty;
            }

            if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
                hits++;
            }
        }

        return ((hits & 1) != 0);
    }

    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    /** The polygon as a path, so the rectangle crossings of Path2D can be reused. */
    private Path2D.Double asPath() {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD, npoints);
        path.moveTo(xpoints[0], ypoints[0]);
        for (int i = 1; i < npoints; i++) {
            path.lineTo(xpoints[i], ypoints[i]);
        }
        path.closePath();
        return path;
    }

    public boolean intersects(double x, double y, double w, double h) {
        if (npoints <= 0 || !getBoundingBox().intersects(x, y, w, h)) {
            return false;
        }
        return asPath().intersects(x, y, w, h);
    }

    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean contains(double x, double y, double w, double h) {
        if (npoints <= 0 || !getBoundingBox().intersects(x, y, w, h)) {
            return false;
        }
        return asPath().contains(x, y, w, h);
    }

    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new PolygonPathIterator(this, at);
    }

    /**
     * The flattening tolerance is ignored, and that is not an omission: a polygon already is a
     * sequence of straight segments, so flattening it cannot change anything.
     */
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    private static class PolygonPathIterator implements PathIterator {

        private Polygon poly;

        private AffineTransform transform;

        private int index;

        PolygonPathIterator(Polygon pg, AffineTransform at) {
            poly = pg;
            transform = at;
            if (pg.npoints == 0) {
                // An empty polygon is already finished: it skips to the end so that the walk does
                // not emit a SEG_CLOSE that closes nothing.
                index = 1;
            }
        }

        public int getWindingRule() {
            // Qualified on purpose: see #469 in COMPILER_FINDINGS.md -- the fields of an
            // implemented interface are not inherited and without the prefix it does not compile.
            return PathIterator.WIND_EVEN_ODD;
        }

        public boolean isDone() {
            return index > poly.npoints;
        }

        public void next() {
            index++;
        }

        public int currentSegment(float[] coords) {
            if (index >= poly.npoints) {
                return PathIterator.SEG_CLOSE;
            }
            coords[0] = poly.xpoints[index];
            coords[1] = poly.ypoints[index];
            if (transform != null) {
                transform.transform(coords, 0, coords, 0, 1);
            }
            return (index == 0 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO);
        }

        public int currentSegment(double[] coords) {
            if (index >= poly.npoints) {
                return PathIterator.SEG_CLOSE;
            }
            coords[0] = poly.xpoints[index];
            coords[1] = poly.ypoints[index];
            if (transform != null) {
                transform.transform(coords, 0, coords, 0, 1);
            }
            return (index == 0 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO);
        }
    }
}
