package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;

// KajiLibrary's java.awt.geom.CubicCurve2D -- a segment of a cubic Bezier curve. The surface is
// complete.
//
// The curve goes from (x1,y1) to (x2,y2) with two control points it **does not touch**: the curve
// passes through the ends and not through the controls. Two things that surprise anyone expecting
// the control quadrilateral to be the shape come out of that:
//
//   * `getBounds2D` returns the curve's **tight** box, not the control polygon's. Both are valid
//     bounds according to `Shape.getBounds2D`, so this is not decided by the contract but by the
//     JDK, which returns the tight one -- checked against the real `java`. The control polygon's was
//     here until the behaviour test did not match. See `CurveBounds`.
//
//   * `contains` measures the region enclosed by the curve **plus the chord** joining its ends, with
//     the even-odd rule. An open curve encloses nothing on its own; closing it with the chord is the
//     only reading that gives "inside" a meaning, and it is the JDK's.
//
// On `solveCubic`: it is solved by Numerical Recipes' (5.6) trigonometric/Cardano method --the same
// one the JDK uses-- and then the roots are polished with two Newton steps. The polishing is not
// decorative: the closed formula loses precision when two roots are close, and without it the double
// roots come out with an error of 1e-8 instead of 1e-15. The **order** the roots are left in is
// specified by the contract neither here nor in the JDK, so comparing them against the JDK demands
// sorting them first; the CgeomAwtTest test does that.
public abstract class CubicCurve2D implements Shape, Cloneable {

    // A curve with float coordinates.
    public static class Float extends CubicCurve2D implements java.io.Serializable {

        public float x1;
        public float y1;
        public float ctrlx1;
        public float ctrly1;
        public float ctrlx2;
        public float ctrly2;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1,
                     float ctrlx1, float ctrly1,
                     float ctrlx2, float ctrly2,
                     float x2, float y2) {
            setCurve(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
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

        public double getCtrlX1() {
            return (double) this.ctrlx1;
        }

        public double getCtrlY1() {
            return (double) this.ctrly1;
        }

        public Point2D getCtrlP1() {
            return Point2D.newFloat(this.ctrlx1, this.ctrly1);
        }

        public double getCtrlX2() {
            return (double) this.ctrlx2;
        }

        public double getCtrlY2() {
            return (double) this.ctrly2;
        }

        public Point2D getCtrlP2() {
            return Point2D.newFloat(this.ctrlx2, this.ctrly2);
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

        public void setCurve(double x1, double y1,
                             double ctrlx1, double ctrly1,
                             double ctrlx2, double ctrly2,
                             double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.ctrlx1 = (float) ctrlx1;
            this.ctrly1 = (float) ctrly1;
            this.ctrlx2 = (float) ctrlx2;
            this.ctrly2 = (float) ctrly2;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        public void setCurve(float x1, float y1,
                             float ctrlx1, float ctrly1,
                             float ctrlx2, float ctrly2,
                             float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    // A curve with double coordinates.
    public static class Double extends CubicCurve2D implements java.io.Serializable {

        public double x1;
        public double y1;
        public double ctrlx1;
        public double ctrly1;
        public double ctrlx2;
        public double ctrly2;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1,
                      double ctrlx1, double ctrly1,
                      double ctrlx2, double ctrly2,
                      double x2, double y2) {
            setCurve(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
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

        public double getCtrlX1() {
            return this.ctrlx1;
        }

        public double getCtrlY1() {
            return this.ctrly1;
        }

        public Point2D getCtrlP1() {
            return Point2D.newDouble(this.ctrlx1, this.ctrly1);
        }

        public double getCtrlX2() {
            return this.ctrlx2;
        }

        public double getCtrlY2() {
            return this.ctrly2;
        }

        public Point2D getCtrlP2() {
            return Point2D.newDouble(this.ctrlx2, this.ctrly2);
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

        public void setCurve(double x1, double y1,
                             double ctrlx1, double ctrly1,
                             double ctrlx2, double ctrly2,
                             double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    protected CubicCurve2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract Point2D getP1();

    public abstract double getCtrlX1();

    public abstract double getCtrlY1();

    public abstract Point2D getCtrlP1();

    public abstract double getCtrlX2();

    public abstract double getCtrlY2();

    public abstract Point2D getCtrlP2();

    public abstract double getX2();

    public abstract double getY2();

    public abstract Point2D getP2();

    public abstract void setCurve(double x1, double y1,
                                  double ctrlx1, double ctrly1,
                                  double ctrlx2, double ctrly2,
                                  double x2, double y2);

    public void setCurve(double[] coords, int offset) {
        setCurve(coords[offset + 0], coords[offset + 1],
                 coords[offset + 2], coords[offset + 3],
                 coords[offset + 4], coords[offset + 5],
                 coords[offset + 6], coords[offset + 7]);
    }

    public void setCurve(Point2D p1, Point2D cp1, Point2D cp2, Point2D p2) {
        setCurve(p1.getX(), p1.getY(), cp1.getX(), cp1.getY(),
                 cp2.getX(), cp2.getY(), p2.getX(), p2.getY());
    }

    public void setCurve(Point2D[] pts, int offset) {
        setCurve(pts[offset + 0].getX(), pts[offset + 0].getY(),
                 pts[offset + 1].getX(), pts[offset + 1].getY(),
                 pts[offset + 2].getX(), pts[offset + 2].getY(),
                 pts[offset + 3].getX(), pts[offset + 3].getY());
    }

    public void setCurve(CubicCurve2D c) {
        setCurve(c.getX1(), c.getY1(), c.getCtrlX1(), c.getCtrlY1(),
                 c.getCtrlX2(), c.getCtrlY2(), c.getX2(), c.getY2());
    }

    // "Flatness": the greater of the two distances from the control points to the chord, squared.
    // It is the measure the flattener decides with whether a piece can be drawn as a segment
    // already.
    public static double getFlatnessSq(double x1, double y1,
                                       double ctrlx1, double ctrly1,
                                       double ctrlx2, double ctrly2,
                                       double x2, double y2) {
        return Math.max(Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx1, ctrly1),
                        Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx2, ctrly2));
    }

    public static double getFlatness(double x1, double y1,
                                     double ctrlx1, double ctrly1,
                                     double ctrlx2, double ctrly2,
                                     double x2, double y2) {
        return Math.sqrt(getFlatnessSq(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2));
    }

    public static double getFlatnessSq(double[] coords, int offset) {
        return getFlatnessSq(coords[offset + 0], coords[offset + 1],
                             coords[offset + 2], coords[offset + 3],
                             coords[offset + 4], coords[offset + 5],
                             coords[offset + 6], coords[offset + 7]);
    }

    public static double getFlatness(double[] coords, int offset) {
        return Math.sqrt(getFlatnessSq(coords, offset));
    }

    public double getFlatnessSq() {
        return getFlatnessSq(getX1(), getY1(), getCtrlX1(), getCtrlY1(),
                             getCtrlX2(), getCtrlY2(), getX2(), getY2());
    }

    public double getFlatness() {
        return Math.sqrt(getFlatnessSq());
    }

    public void subdivide(CubicCurve2D left, CubicCurve2D right) {
        subdivide(this, left, right);
    }

    // A De Casteljau cut at t=0.5. It is done with additions alone and one division by 2, which is
    // exact in binary: subdividing introduces no rounding error of its own.
    public static void subdivide(CubicCurve2D src, CubicCurve2D left, CubicCurve2D right) {
        double x1 = src.getX1();
        double y1 = src.getY1();
        double ctrlx1 = src.getCtrlX1();
        double ctrly1 = src.getCtrlY1();
        double ctrlx2 = src.getCtrlX2();
        double ctrly2 = src.getCtrlY2();
        double x2 = src.getX2();
        double y2 = src.getY2();
        double centerx = (ctrlx1 + ctrlx2) / 2.0;
        double centery = (ctrly1 + ctrly2) / 2.0;
        ctrlx1 = (x1 + ctrlx1) / 2.0;
        ctrly1 = (y1 + ctrly1) / 2.0;
        ctrlx2 = (x2 + ctrlx2) / 2.0;
        ctrly2 = (y2 + ctrly2) / 2.0;
        double ctrlx12 = (ctrlx1 + centerx) / 2.0;
        double ctrly12 = (ctrly1 + centery) / 2.0;
        double ctrlx21 = (ctrlx2 + centerx) / 2.0;
        double ctrly21 = (ctrly2 + centery) / 2.0;
        centerx = (ctrlx12 + ctrlx21) / 2.0;
        centery = (ctrly12 + ctrly21) / 2.0;
        if (left != null) {
            left.setCurve(x1, y1, ctrlx1, ctrly1, ctrlx12, ctrly12, centerx, centery);
        }
        if (right != null) {
            right.setCurve(centerx, centery, ctrlx21, ctrly21, ctrlx2, ctrly2, x2, y2);
        }
    }

    public static void subdivide(double[] src, int srcoff,
                                 double[] left, int leftoff,
                                 double[] right, int rightoff) {
        double x1 = src[srcoff + 0];
        double y1 = src[srcoff + 1];
        double ctrlx1 = src[srcoff + 2];
        double ctrly1 = src[srcoff + 3];
        double ctrlx2 = src[srcoff + 4];
        double ctrly2 = src[srcoff + 5];
        double x2 = src[srcoff + 6];
        double y2 = src[srcoff + 7];
        if (left != null) {
            left[leftoff + 0] = x1;
            left[leftoff + 1] = y1;
        }
        if (right != null) {
            right[rightoff + 6] = x2;
            right[rightoff + 7] = y2;
        }
        x1 = (x1 + ctrlx1) / 2.0;
        y1 = (y1 + ctrly1) / 2.0;
        x2 = (x2 + ctrlx2) / 2.0;
        y2 = (y2 + ctrly2) / 2.0;
        double centerx = (ctrlx1 + ctrlx2) / 2.0;
        double centery = (ctrly1 + ctrly2) / 2.0;
        ctrlx1 = (x1 + centerx) / 2.0;
        ctrly1 = (y1 + centery) / 2.0;
        ctrlx2 = (x2 + centerx) / 2.0;
        ctrly2 = (y2 + centery) / 2.0;
        centerx = (ctrlx1 + ctrlx2) / 2.0;
        centery = (ctrly1 + ctrly2) / 2.0;
        if (left != null) {
            left[leftoff + 2] = x1;
            left[leftoff + 3] = y1;
            left[leftoff + 4] = ctrlx1;
            left[leftoff + 5] = ctrly1;
            left[leftoff + 6] = centerx;
            left[leftoff + 7] = centery;
        }
        if (right != null) {
            right[rightoff + 0] = centerx;
            right[rightoff + 1] = centery;
            right[rightoff + 2] = ctrlx2;
            right[rightoff + 3] = ctrly2;
            right[rightoff + 4] = x2;
            right[rightoff + 5] = y2;
        }
    }

    public static int solveCubic(double[] eqn) {
        return solveCubic(eqn, eqn);
    }

    // eqn = {c, b, a, d} with d*t^3 + a*t^2 + b*t + c = 0. It returns how many real roots there are
    // and leaves them in `res`. Returning -1 means "infinitely many" (the equation is 0 = 0).
    public static int solveCubic(double[] eqn, double[] res) {
        double d = eqn[3];
        if (d == 0.0) {
            return QuadCurve2D.solveQuadratic(eqn, res);
        }
        double a = eqn[2] / d;
        double b = eqn[1] / d;
        double c = eqn[0] / d;
        // The original coefficients are copied before writing into `res`, which may be the same
        // array: Newton's polishing needs them intact.
        double[] orig = new double[4];
        orig[0] = eqn[0];
        orig[1] = eqn[1];
        orig[2] = eqn[2];
        orig[3] = eqn[3];

        int roots = 0;
        double q = (a * a - 3.0 * b) / 9.0;
        double r = (2.0 * a * a * a - 9.0 * a * b + 27.0 * c) / 54.0;
        double r2 = r * r;
        double q3 = q * q * q;
        double a3 = a / 3.0;
        if (r2 < q3) {
            // Three distinct real roots: the trigonometric form.
            double theta = Math.acos(r / Math.sqrt(q3));
            double m = -2.0 * Math.sqrt(q);
            res[0] = m * Math.cos(theta / 3.0) - a3;
            res[1] = m * Math.cos((theta + Math.PI * 2.0) / 3.0) - a3;
            res[2] = m * Math.cos((theta - Math.PI * 2.0) / 3.0) - a3;
            roots = 3;
        } else {
            // One real root: Cardano's form.
            boolean neg = (r < 0.0);
            double s = Math.sqrt(r2 - q3);
            double rr = r;
            if (neg) {
                rr = -rr;
            }
            double aa = Math.pow(rr + s, 1.0 / 3.0);
            if (!neg) {
                aa = -aa;
            }
            double bb;
            if (aa == 0.0) {
                bb = 0.0;
            } else {
                bb = q / aa;
            }
            res[0] = (aa + bb) - a3;
            roots = 1;
        }
        for (int i = 0; i < roots; i = i + 1) {
            res[i] = refine(orig, res[i]);
        }
        return roots;
    }

    // Two Newton steps over the original polynomial. It aborts if the derivative is zero or if the
    // step does not improve: over-polishing can push the root away once the double's limit has been
    // reached.
    private static double refine(double[] eqn, double t) {
        for (int k = 0; k < 2; k = k + 1) {
            double f = ((eqn[3] * t + eqn[2]) * t + eqn[1]) * t + eqn[0];
            double df = (3.0 * eqn[3] * t + 2.0 * eqn[2]) * t + eqn[1];
            if (df == 0.0) {
                return t;
            }
            double next = t - f / df;
            if (next == t) {
                return t;
            }
            double fnext = ((eqn[3] * next + eqn[2]) * next + eqn[1]) * next + eqn[0];
            if (Math.abs(fnext) > Math.abs(f)) {
                return t;
            }
            t = next;
        }
        return t;
    }

    // "Inside" is the region enclosed by the curve and the chord between its ends, with the
    // even-odd rule. An infinite or NaN x or y gives false: there is no point to examine.
    public boolean contains(double x, double y) {
        if (!(x * 0.0 + y * 0.0 == 0.0)) {
            return false;
        }
        double x1 = getX1();
        double y1 = getY1();
        double x2 = getX2();
        double y2 = getY2();
        int crossings = Curve.pointCrossingsForLine(x, y, x1, y1, x2, y2)
                + Curve.pointCrossingsForCubic(x, y, x1, y1,
                                               getCtrlX1(), getCtrlY1(),
                                               getCtrlX2(), getCtrlY2(),
                                               x2, y2, 0);
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
        // It is enough for the count not to be zero: RECT_INTERSECTS is not zero either, and both
        // things --an edge touched or a non-empty interior-- mean they intersect.
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
        // Here they do have to be told apart: RECT_INTERSECTS means the curve's edge enters the
        // rectangle, and then the rectangle is not contained even if the crossings are not zero.
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
        // The curve is walked backwards so that its direction closes with the chord's, which was
        // already counted in the opposite direction. With both going the same way the crossings
        // cancel and `contains` would give false for everything.
        return Curve.rectCrossingsForCubic(crossings, x, y, x + w, y + h,
                                           getX2(), getY2(),
                                           getCtrlX2(), getCtrlY2(),
                                           getCtrlX1(), getCtrlY1(),
                                           getX1(), getY1(), 0);
    }

    /**
     * The curve's **tight** box: the smallest one containing it.
     *
     * <p>It is not the control polygon's, which would be a valid bound and easier to work out. It is
     * what the JDK returns, checked by running the same case with the real `java`. See
     * {@link CurveBounds}, which solves the derivatives.
     *
     * <p>The JDK declares this concrete method here and not in the nested subclasses, so `Float`
     * also returns a `Rectangle2D.Double`; that is honoured.
     */
    public Rectangle2D getBounds2D() {
        double[] xs = CurveBounds.cubic(getX1(), getCtrlX1(), getCtrlX2(), getX2());
        double[] ys = CurveBounds.cubic(getY1(), getCtrlY1(), getCtrlY2(), getY2());
        return Rectangle2D.newDouble(xs[0], ys[0], xs[1] - xs[0], ys[1] - ys[0]);
    }

    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new CubicIterator(this, at);
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
