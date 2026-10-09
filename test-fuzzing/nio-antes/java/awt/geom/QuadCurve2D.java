package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;

// KajiLibrary's java.awt.geom.QuadCurve2D -- a segment of a quadratic Bezier curve. The surface is
// complete.
//
// The same holds as in CubicCurve2D and for the same reasons: `getBounds2D` is the control
// triangle's box (a valid upper bound by the convex hull, not the minimal box) and `contains`
// measures the region enclosed by the curve and the chord between the ends, with the even-odd rule.
//
// `solveQuadratic` has a detail that is often implemented wrongly: it **returns -1**, not 0, when
// the equation is 0 = 0 (every coefficient null). -1 means "infinitely many roots"; returning 0
// would be saying there is none, which is the opposite. And with a == 0 the equation is linear and
// has to be solved as such instead of dividing by zero.
public abstract class QuadCurve2D implements Shape, Cloneable {

    // A curve with float coordinates.
    public static class Float extends QuadCurve2D implements java.io.Serializable {

        public float x1;
        public float y1;
        public float ctrlx;
        public float ctrly;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float ctrlx, float ctrly, float x2, float y2) {
            setCurve(x1, y1, ctrlx, ctrly, x2, y2);
        }

        public double getX1() {
            return (double) this.x1;
        }

        public double getY1() {
            return (double) this.y1;
        }

        public Point2D getP1() {
            return Point2D.newFloat(this.x1, this.y1);
        }

        public double getCtrlX() {
            return (double) this.ctrlx;
        }

        public double getCtrlY() {
            return (double) this.ctrly;
        }

        public Point2D getCtrlPt() {
            return Point2D.newFloat(this.ctrlx, this.ctrly);
        }

        public double getX2() {
            return (double) this.x2;
        }

        public double getY2() {
            return (double) this.y2;
        }

        public Point2D getP2() {
            return Point2D.newFloat(this.x2, this.y2);
        }

        public void setCurve(double x1, double y1, double ctrlx, double ctrly,
                             double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.ctrlx = (float) ctrlx;
            this.ctrly = (float) ctrly;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        public void setCurve(float x1, float y1, float ctrlx, float ctrly, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx = ctrlx;
            this.ctrly = ctrly;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    // A curve with double coordinates.
    public static class Double extends QuadCurve2D implements java.io.Serializable {

        public double x1;
        public double y1;
        public double ctrlx;
        public double ctrly;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
            setCurve(x1, y1, ctrlx, ctrly, x2, y2);
        }

        public double getX1() {
            return this.x1;
        }

        public double getY1() {
            return this.y1;
        }

        public Point2D getP1() {
            return Point2D.newDouble(this.x1, this.y1);
        }

        public double getCtrlX() {
            return this.ctrlx;
        }

        public double getCtrlY() {
            return this.ctrly;
        }

        public Point2D getCtrlPt() {
            return Point2D.newDouble(this.ctrlx, this.ctrly);
        }

        public double getX2() {
            return this.x2;
        }

        public double getY2() {
            return this.y2;
        }

        public Point2D getP2() {
            return Point2D.newDouble(this.x2, this.y2);
        }

        public void setCurve(double x1, double y1, double ctrlx, double ctrly,
                             double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx = ctrlx;
            this.ctrly = ctrly;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    protected QuadCurve2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract Point2D getP1();

    public abstract double getCtrlX();

    public abstract double getCtrlY();

    public abstract Point2D getCtrlPt();

    public abstract double getX2();

    public abstract double getY2();

    public abstract Point2D getP2();

    public abstract void setCurve(double x1, double y1, double ctrlx, double ctrly,
                                  double x2, double y2);

    public void setCurve(double[] coords, int offset) {
        setCurve(coords[offset + 0], coords[offset + 1],
                 coords[offset + 2], coords[offset + 3],
                 coords[offset + 4], coords[offset + 5]);
    }

    public void setCurve(Point2D p1, Point2D cp, Point2D p2) {
        setCurve(p1.getX(), p1.getY(), cp.getX(), cp.getY(), p2.getX(), p2.getY());
    }

    public void setCurve(Point2D[] pts, int offset) {
        setCurve(pts[offset + 0].getX(), pts[offset + 0].getY(),
                 pts[offset + 1].getX(), pts[offset + 1].getY(),
                 pts[offset + 2].getX(), pts[offset + 2].getY());
    }

    public void setCurve(QuadCurve2D c) {
        setCurve(c.getX1(), c.getY1(), c.getCtrlX(), c.getCtrlY(), c.getX2(), c.getY2());
    }

    // The (squared) distance from the control point to the chord: how far the curve departs from a
    // segment. It is the measure the flattener decides with whether a piece can be drawn straight
    // already.
    public static double getFlatnessSq(double x1, double y1, double ctrlx, double ctrly,
                                       double x2, double y2) {
        return Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx, ctrly);
    }

    public static double getFlatness(double x1, double y1, double ctrlx, double ctrly,
                                     double x2, double y2) {
        return Line2D.ptSegDist(x1, y1, x2, y2, ctrlx, ctrly);
    }

    public static double getFlatnessSq(double[] coords, int offset) {
        return Line2D.ptSegDistSq(coords[offset + 0], coords[offset + 1],
                                  coords[offset + 4], coords[offset + 5],
                                  coords[offset + 2], coords[offset + 3]);
    }

    public static double getFlatness(double[] coords, int offset) {
        return Line2D.ptSegDist(coords[offset + 0], coords[offset + 1],
                                coords[offset + 4], coords[offset + 5],
                                coords[offset + 2], coords[offset + 3]);
    }

    public double getFlatnessSq() {
        return Line2D.ptSegDistSq(getX1(), getY1(), getX2(), getY2(), getCtrlX(), getCtrlY());
    }

    public double getFlatness() {
        return Line2D.ptSegDist(getX1(), getY1(), getX2(), getY2(), getCtrlX(), getCtrlY());
    }

    public void subdivide(QuadCurve2D left, QuadCurve2D right) {
        subdivide(this, left, right);
    }

    // A De Casteljau cut at t=0.5: only additions and divisions by 2, exact in binary.
    public static void subdivide(QuadCurve2D src, QuadCurve2D left, QuadCurve2D right) {
        double x1 = src.getX1();
        double y1 = src.getY1();
        double ctrlx = src.getCtrlX();
        double ctrly = src.getCtrlY();
        double x2 = src.getX2();
        double y2 = src.getY2();
        double ctrlx1 = (x1 + ctrlx) / 2.0;
        double ctrly1 = (y1 + ctrly) / 2.0;
        double ctrlx2 = (x2 + ctrlx) / 2.0;
        double ctrly2 = (y2 + ctrly) / 2.0;
        double centerx = (ctrlx1 + ctrlx2) / 2.0;
        double centery = (ctrly1 + ctrly2) / 2.0;
        if (left != null) {
            left.setCurve(x1, y1, ctrlx1, ctrly1, centerx, centery);
        }
        if (right != null) {
            right.setCurve(centerx, centery, ctrlx2, ctrly2, x2, y2);
        }
    }

    public static void subdivide(double[] src, int srcoff,
                                 double[] left, int leftoff,
                                 double[] right, int rightoff) {
        double x1 = src[srcoff + 0];
        double y1 = src[srcoff + 1];
        double ctrlx = src[srcoff + 2];
        double ctrly = src[srcoff + 3];
        double x2 = src[srcoff + 4];
        double y2 = src[srcoff + 5];
        if (left != null) {
            left[leftoff + 0] = x1;
            left[leftoff + 1] = y1;
        }
        if (right != null) {
            right[rightoff + 4] = x2;
            right[rightoff + 5] = y2;
        }
        x1 = (x1 + ctrlx) / 2.0;
        y1 = (y1 + ctrly) / 2.0;
        x2 = (x2 + ctrlx) / 2.0;
        y2 = (y2 + ctrly) / 2.0;
        ctrlx = (x1 + x2) / 2.0;
        ctrly = (y1 + y2) / 2.0;
        if (left != null) {
            left[leftoff + 2] = x1;
            left[leftoff + 3] = y1;
            left[leftoff + 4] = ctrlx;
            left[leftoff + 5] = ctrly;
        }
        if (right != null) {
            right[rightoff + 0] = ctrlx;
            right[rightoff + 1] = ctrly;
            right[rightoff + 2] = x2;
            right[rightoff + 3] = y2;
        }
    }

    public static int solveQuadratic(double[] eqn) {
        return solveQuadratic(eqn, eqn);
    }

    // eqn = {c, b, a} with a*t^2 + b*t + c = 0. It returns how many real roots there are, or -1 if
    // the equation is satisfied for every t.
    public static int solveQuadratic(double[] eqn, double[] res) {
        double a = eqn[2];
        double b = eqn[1];
        double c = eqn[0];
        int roots = 0;
        if (a == 0.0) {
            // It is not quadratic: it is linear. And if there is no term in t either, the equation
            // is a **constant**: the contract says to return -1 there, whether that constant is zero
            // (infinitely many solutions) or not (none). The two cases are not told apart because
            // the contract does not tell them apart and neither does the JDK; -1 means "this is not
            // an equation in t".
            if (b == 0.0) {
                return -1;
            }
            res[roots] = -c / b;
            roots = roots + 1;
        } else {
            double d = b * b - 4.0 * a * c;
            if (d < 0.0) {
                return 0;
            }
            d = Math.sqrt(d);
            // The "stable" form of the quadratic: the root whose numerator does not subtract
            // similar quantities is worked out first and the other comes from the product of the
            // roots (c/a). With -b+sqrt(d) straight, digits are lost when b^2 >> 4ac.
            double q;
            if (b < 0.0) {
                q = (-b + d) / 2.0;
            } else {
                q = (-b - d) / 2.0;
            }
            res[roots] = q / a;
            roots = roots + 1;
            if (q != 0.0) {
                res[roots] = c / q;
                roots = roots + 1;
            }
        }
        return roots;
    }

    // "Inside" is the region enclosed by the curve and the chord between its ends, with the
    // even-odd rule. An infinite or NaN x or y gives false.
    public boolean contains(double x, double y) {
        if (!(x * 0.0 + y * 0.0 == 0.0)) {
            return false;
        }
        double x1 = getX1();
        double y1 = getY1();
        double xc = getCtrlX();
        double yc = getCtrlY();
        double x2 = getX2();
        double y2 = getY2();
        int crossings = Curve.pointCrossingsForLine(x, y, x1, y1, x2, y2)
                + Curve.pointCrossingsForQuad(x, y, x1, y1, xc, yc, x2, y2, 0);
        return ((crossings & 1) == 1);
    }

    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    public boolean intersects(double x, double y, double w, double h) {
        if (w <= 0.0 || h <= 0.0) {
            return false;
        }
        int numCrossings = rectCrossings(x, y, w, h);
        return numCrossings != 0;
    }

    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean contains(double x, double y, double w, double h) {
        if (w <= 0.0 || h <= 0.0) {
            return false;
        }
        int numCrossings = rectCrossings(x, y, w, h);
        return !(numCrossings == 0 || numCrossings == Curve.RECT_INTERSECTS);
    }

    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    private int rectCrossings(double x, double y, double w, double h) {
        int crossings = 0;
        if (!(getX1() == getX2() && getY1() == getY2())) {
            crossings = Curve.rectCrossingsForLine(crossings, x, y, x + w, y + h,
                                                   getX1(), getY1(), getX2(), getY2());
            if (crossings == Curve.RECT_INTERSECTS) {
                return crossings;
            }
        }
        // The curve is walked backwards so as to close the contour with the chord, which was
        // already counted the other way round. See the same note in CubicCurve2D.
        return Curve.rectCrossingsForQuad(crossings, x, y, x + w, y + h,
                                          getX2(), getY2(),
                                          getCtrlX(), getCtrlY(),
                                          getX1(), getY1(), 0);
    }

    /**
     * The curve's **tight** box: the smallest one containing it.
     *
     * <p>See {@link CubicCurve2D#getBounds2D} and {@link CurveBounds}: same criterion, same reason.
     */
    public Rectangle2D getBounds2D() {
        double[] xs = CurveBounds.quad(getX1(), getCtrlX(), getX2());
        double[] ys = CurveBounds.quad(getY1(), getCtrlY(), getY2());
        return Rectangle2D.newDouble(xs[0], ys[0], xs[1] - xs[0], ys[1] - ys[0]);
    }

    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new QuadIterator(this, at);
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(getPathIterator(at), flatness);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
}
