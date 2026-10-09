package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;

// KajiLibrary's java.awt.geom.Area -- a region of the plane closed under union, intersection,
// difference and symmetric difference. The surface is complete.
//
// What sets Area apart from Path2D is not the four boolean operations but the **normal form**: an
// Area does not keep the path it was given, it keeps the edge of the region that path encloses, with
// the non-zero rule, with no crossings and with no stretches that overlap. That is why
// `new Area(shape)` already does work even if nothing is operated on, and why `isEmpty`,
// `isPolygonal`, `isRectangular`, `isSingular` and `equals` mean something: they ask about the
// region, not about how it was written. A figure-of-eight path that cancels itself out gives an
// empty Area; two abutting rectangles give a single loop with no shared side.
//
// The normalization and the four operations are the same computation with a different boolean
// function, and they live in AreaOp. That file's header explains why it is done in three passes and
// not like the JDK's sweep.
//
// Two observable things worth knowing:
//
//   * **The loops' orientation is not the JDK's.** Here the interior ends up to the right of the
//     piece walked top to bottom, that is, winding +1; the JDK chooses the opposite. Both fill the
//     same region with the non-zero rule --which is all the contract promises-- and nothing in the
//     API exposes the sign. It is noted because a `getPathIterator` compared coordinate by
//     coordinate against the JDK is going to differ in the walking order.
//
//   * **`equals(Area)` is not `equals(Object)`.** There is no override of `equals(Object)`, just as
//     in the JDK: `anArea.equals((Object) another)` falls back on Object's identity. It is an
//     inherited oddity and it is honoured on purpose, because a `List.contains` or a `HashSet` that
//     started using geometric equality would be an observable change of behaviour.
//
// Nothing was left out of this class.
public class Area implements Shape, Cloneable {

    // The already normalized edge. Always WIND_NON_ZERO and always with every subpath closed.
    private Path2D path;

    /** An empty region. */
    public Area() {
        this.path = Path2D.newDouble(PathIterator.WIND_NON_ZERO);
    }

    /**
     * The region enclosed by `s`, normalized.
     *
     * The open subpaths are closed implicitly: the area enclosed by a path with no `closePath` is
     * the closed path's, which is what Shape's spec says.
     */
    public Area(Shape s) {
        if (s == null) {
            throw new NullPointerException("s");
        }
        if (s instanceof Area) {
            // It is normalized already: copying it is cheaper and gives exactly the same thing.
            this.path = Path2D.newDouble(((Area) s).path, null);
        } else {
            ArrayList<AreaCurve> curves = new ArrayList<AreaCurve>();
            PathIterator pi = s.getPathIterator(null);
            int rule = pi.getWindingRule();
            AreaCurve.appendPath(curves, pi, AreaCurve.LEFT);
            this.path = AreaOp.compute(curves, rule, PathIterator.WIND_NON_ZERO, AreaOp.ADD);
        }
    }

    // --- boolean operations ----------------------------------------------------------------------

    /** Union with `rhs`. */
    public void add(Area rhs) {
        applyOp(rhs, AreaOp.ADD);
    }

    /** Subtraction of `rhs`. */
    public void subtract(Area rhs) {
        applyOp(rhs, AreaOp.SUB);
    }

    /** Intersection with `rhs`. */
    public void intersect(Area rhs) {
        applyOp(rhs, AreaOp.INT);
    }

    /** Symmetric difference with `rhs`: what is in one and not in the other. */
    public void exclusiveOr(Area rhs) {
        applyOp(rhs, AreaOp.XOR);
    }

    private void applyOp(Area rhs, int op) {
        if (rhs == null) {
            throw new NullPointerException("rhs");
        }
        this.path = combine(this, rhs, op);
    }

    private static Path2D combine(Area left, Area right, int op) {
        ArrayList<AreaCurve> curves = new ArrayList<AreaCurve>();
        AreaCurve.appendPath(curves, left.path.getPathIterator(null), AreaCurve.LEFT);
        AreaCurve.appendPath(curves, right.path.getPathIterator(null), AreaCurve.RIGHT);
        return AreaOp.compute(curves, PathIterator.WIND_NON_ZERO,
                PathIterator.WIND_NON_ZERO, op);
    }

    /** Leaves the region empty. */
    public void reset() {
        this.path = Path2D.newDouble(PathIterator.WIND_NON_ZERO);
    }

    // --- questions about the region --------------------------------------------------------------

    /** Whether the region encloses nothing. */
    public boolean isEmpty() {
        return this.path.getPathIterator(null).isDone();
    }

    /** Whether the edge is all straight segments. */
    public boolean isPolygonal() {
        PathIterator pi = this.path.getPathIterator(null);
        double[] coords = new double[6];
        while (!pi.isDone()) {
            int seg = pi.currentSegment(coords);
            if (seg == PathIterator.SEG_QUADTO || seg == PathIterator.SEG_CUBICTO) {
                return false;
            }
            pi.next();
        }
        return true;
    }

    /**
     * Whether the region is a rectangle with its sides parallel to the axes.
     *
     * It is checked against the bounding rectangle and not by counting vertices: a polygonal edge
     * may have extra vertices in the middle of a side --the union of two rectangles sharing part of
     * a side leaves them-- and go on being geometrically a rectangle. Comparing the two regions
     * answers the question the method's name asks.
     */
    public boolean isRectangular() {
        if (isEmpty()) {
            return true;
        }
        if (!isPolygonal() || !isSingular()) {
            return false;
        }
        return new Area(getBounds2D()).equals(this);
    }

    /** Whether the region is of a single piece with no holes, that is, a single loop. */
    public boolean isSingular() {
        PathIterator pi = this.path.getPathIterator(null);
        double[] coords = new double[6];
        int loops = 0;
        while (!pi.isDone()) {
            if (pi.currentSegment(coords) == PathIterator.SEG_MOVETO) {
                loops = loops + 1;
                if (loops > 1) {
                    return false;
                }
            }
            pi.next();
        }
        return true;
    }

    /**
     * Whether `other` encloses exactly the same region.
     *
     * The symmetric difference is worked out and it is asked whether it came out empty. Comparing
     * the two edges directly is good as a shortcut --the same region's two normal forms agree when
     * they were arrived at by the same route-- but not as an answer: two equal regions may have the
     * edge split into different pieces according to what they were operated with before.
     */
    public boolean equals(Area other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (sameOutline(this.path, other.path)) {
            return true;
        }
        return combine(this, other, AreaOp.XOR).getPathIterator(null).isDone();
    }

    private static boolean sameOutline(Path2D a, Path2D b) {
        PathIterator pa = a.getPathIterator(null);
        PathIterator pb = b.getPathIterator(null);
        double[] ca = new double[6];
        double[] cb = new double[6];
        while (!pa.isDone() && !pb.isDone()) {
            int sa = pa.currentSegment(ca);
            int sb = pb.currentSegment(cb);
            if (sa != sb) {
                return false;
            }
            int n = 0;
            if (sa == PathIterator.SEG_MOVETO || sa == PathIterator.SEG_LINETO) {
                n = 2;
            } else if (sa == PathIterator.SEG_QUADTO) {
                n = 4;
            } else if (sa == PathIterator.SEG_CUBICTO) {
                n = 6;
            }
            int i = 0;
            while (i < n) {
                if (ca[i] != cb[i]) {
                    return false;
                }
                i = i + 1;
            }
            pa.next();
            pb.next();
        }
        return pa.isDone() && pb.isDone();
    }

    // --- transformation --------------------------------------------------------------------------

    /**
     * Applies the transform in place.
     *
     * It renormalizes after transforming and does not just move the points: a reflection turns every
     * loop's walking direction around, and a singular matrix flattens the region against a line and
     * leaves it with no area. Both cases are settled by the normalization, which is the same one as
     * `new Area(Shape)`'s.
     */
    public void transform(AffineTransform t) {
        if (t == null) {
            throw new NullPointerException("transform");
        }
        this.path.transform(t);
        ArrayList<AreaCurve> curves = new ArrayList<AreaCurve>();
        AreaCurve.appendPath(curves, this.path.getPathIterator(null), AreaCurve.LEFT);
        this.path = AreaOp.compute(curves, PathIterator.WIND_NON_ZERO,
                PathIterator.WIND_NON_ZERO, AreaOp.ADD);
    }

    /** A transformed copy, without touching this one. */
    public Area createTransformedArea(AffineTransform t) {
        Area a = new Area(this);
        a.transform(t);
        return a;
    }

    public Object clone() {
        return new Area(this);
    }

    // --- Shape -----------------------------------------------------------------------------------

    public Rectangle getBounds() {
        return this.path.getBounds();
    }

    public Rectangle2D getBounds2D() {
        return this.path.getBounds2D();
    }

    public boolean contains(double x, double y) {
        return this.path.contains(x, y);
    }

    public boolean contains(Point2D p) {
        if (p == null) {
            throw new NullPointerException("p");
        }
        return contains(p.getX(), p.getY());
    }

    public boolean contains(double x, double y, double w, double h) {
        return this.path.contains(x, y, w, h);
    }

    public boolean contains(Rectangle2D r) {
        if (r == null) {
            throw new NullPointerException("r");
        }
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean intersects(double x, double y, double w, double h) {
        return this.path.intersects(x, y, w, h);
    }

    public boolean intersects(Rectangle2D r) {
        if (r == null) {
            throw new NullPointerException("r");
        }
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return this.path.getPathIterator(at);
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(this.path.getPathIterator(at), flatness);
    }
}
