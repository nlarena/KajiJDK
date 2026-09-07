package java.awt.geom;

import java.util.ArrayList;

// A piece of edge monotonic in Y (not API). It is the piece AreaOp works on.
//
// All of Area's machinery rests on a single invariant: **each piece is monotonic in Y and is kept
// top to bottom**, with `dir` remembering whether the original stroke went downwards (+1) or upwards
// (-1). That invariant buys three things that would otherwise be loose special cases:
//
//   * `xForY(y)` is well defined --there is a single x for each y-- and is solved by bisection on
//     the parameter. Over a monotonic function bisection converges to the ulp in 60 steps and does
//     not need to solve the cubic: there is no closed formula more exact than that, only faster.
//
//   * The crossing count for the winding number is the usual one --a half-open interval
//     [ytop, ybot) and a sum of `dir`-- and gives the exact result even when the ray passes right
//     through a vertex: at a local maximum or minimum two pieces are born with the same `ytop` and
//     opposite `dir`, which cancel; at a pass-through vertex one ends (does not count) and another
//     begins (counts), that is, once.
//
//   * The horizontal stretches are discarded on construction. They contribute nothing to the winding
//     number and they are the classic source of divisions by zero. AreaOp manufactures them again at
//     the end, already classified, to close the loops.
//
// The price of the invariant is splitting each quadratic and each cubic at its Y extrema before
// storing them. That is one quadratic to solve (dy/dt = 0) and at most two cuts.
final class AreaCurve {

    /** A piece of the left operand. */
    static final int LEFT = 0;

    /** A piece of the right operand. */
    static final int RIGHT = 1;

    /** The order: 1 line, 2 quadratic, 3 cubic. */
    final int order;

    /** 2*(order+1) coordinates, from the top end to the bottom one. */
    final double[] c;

    /** +1 if the original stroke went down, -1 if it went up. */
    final int dir;

    /** LEFT o RIGHT. */
    final int tag;

    AreaCurve(int order, double[] c, int dir, int tag) {
        this.order = order;
        this.c = c;
        this.dir = dir;
        this.tag = tag;
    }

    double xtop() {
        return this.c[0];
    }

    double ytop() {
        return this.c[1];
    }

    double xbot() {
        return this.c[2 * this.order];
    }

    double ybot() {
        return this.c[2 * this.order + 1];
    }

    // --- evaluation ------------------------------------------------------------------------------

    /** The curve's point at the parameter t, by de Casteljau. */
    double[] point(double t) {
        double[] p = copyOf(this.c);
        int level = this.order;
        while (level > 0) {
            int i = 0;
            while (i < level) {
                p[2 * i] = p[2 * i] + (p[2 * i + 2] - p[2 * i]) * t;
                p[2 * i + 1] = p[2 * i + 1] + (p[2 * i + 3] - p[2 * i + 1]) * t;
                i = i + 1;
            }
            level = level - 1;
        }
        double[] r = new double[2];
        r[0] = p[0];
        r[1] = p[1];
        return r;
    }

    // The X corresponding to that Y. Outside the range it returns the end, which is what the
    // callers want: the curve "is worth" its tip when the ray passes right through it.
    double xForY(double y) {
        if (y <= ytop()) {
            return xtop();
        }
        if (y >= ybot()) {
            return xbot();
        }
        if (this.order == 1) {
            double dy = this.c[3] - this.c[1];
            double t = (y - this.c[1]) / dy;
            return this.c[0] + (this.c[2] - this.c[0]) * t;
        }
        // Bisection: y(t) is monotonically increasing by construction, so the invariant
        // y(lo) <= y <= y(hi) holds without looking at derivatives.
        double lo = 0.0;
        double hi = 1.0;
        int i = 0;
        while (i < 60) {
            double m = (lo + hi) * 0.5;
            double[] p = point(m);
            if (p[1] < y) {
                lo = m;
            } else {
                hi = m;
            }
            i = i + 1;
        }
        double[] p = point((lo + hi) * 0.5);
        return p[0];
    }

    // --- subdivision -----------------------------------------------------------------------------

    static double[] copyOf(double[] a) {
        double[] r = new double[a.length];
        int i = 0;
        while (i < a.length) {
            r[i] = a[i];
            i = i + 1;
        }
        return r;
    }

    /** The [0, t] piece's control points. */
    static double[] leftPart(double[] c, int n, double t) {
        double[] p = copyOf(c);
        double[] out = new double[2 * (n + 1)];
        out[0] = p[0];
        out[1] = p[1];
        int level = n;
        int k = 1;
        while (level > 0) {
            int i = 0;
            while (i < level) {
                p[2 * i] = p[2 * i] + (p[2 * i + 2] - p[2 * i]) * t;
                p[2 * i + 1] = p[2 * i + 1] + (p[2 * i + 3] - p[2 * i + 1]) * t;
                i = i + 1;
            }
            out[2 * k] = p[0];
            out[2 * k + 1] = p[1];
            k = k + 1;
            level = level - 1;
        }
        return out;
    }

    /** The [t, 1] piece's control points. */
    static double[] rightPart(double[] c, int n, double t) {
        double[] p = copyOf(c);
        double[] out = new double[2 * (n + 1)];
        out[2 * n] = p[2 * n];
        out[2 * n + 1] = p[2 * n + 1];
        int level = n;
        int k = n - 1;
        while (level > 0) {
            int i = 0;
            while (i < level) {
                p[2 * i] = p[2 * i] + (p[2 * i + 2] - p[2 * i]) * t;
                p[2 * i + 1] = p[2 * i + 1] + (p[2 * i + 3] - p[2 * i + 1]) * t;
                i = i + 1;
            }
            out[2 * k] = p[2 * (level - 1)];
            out[2 * k + 1] = p[2 * (level - 1) + 1];
            k = k - 1;
            level = level - 1;
        }
        return out;
    }

    /** The [t0, t1] piece's control points. */
    static double[] subCurve(double[] c, int n, double t0, double t1) {
        double[] r = c;
        if (t1 < 1.0) {
            r = leftPart(r, n, t1);
        }
        if (t0 > 0.0) {
            double s = 0.0;
            if (t1 > 0.0) {
                s = t0 / t1;
            }
            if (s > 1.0) {
                s = 1.0;
            }
            r = rightPart(r, n, s);
        } else {
            r = copyOf(r);
        }
        return r;
    }

    // --- construction from a path ----------------------------------------------------------------

    static void appendPath(ArrayList<AreaCurve> out, PathIterator pi, int tag) {
        double[] coords = new double[6];
        double movx = 0.0;
        double movy = 0.0;
        double curx = 0.0;
        double cury = 0.0;
        boolean open = false;
        while (!pi.isDone()) {
            int seg = pi.currentSegment(coords);
            if (seg == PathIterator.SEG_MOVETO) {
                if (open) {
                    appendLine(out, curx, cury, movx, movy, tag);
                }
                movx = coords[0];
                movy = coords[1];
                curx = movx;
                cury = movy;
                open = true;
            } else if (seg == PathIterator.SEG_LINETO) {
                appendLine(out, curx, cury, coords[0], coords[1], tag);
                curx = coords[0];
                cury = coords[1];
            } else if (seg == PathIterator.SEG_QUADTO) {
                appendQuad(out, curx, cury, coords[0], coords[1], coords[2], coords[3], tag);
                curx = coords[2];
                cury = coords[3];
            } else if (seg == PathIterator.SEG_CUBICTO) {
                appendCubic(out, curx, cury, coords[0], coords[1], coords[2], coords[3],
                        coords[4], coords[5], tag);
                curx = coords[4];
                cury = coords[5];
            } else {
                appendLine(out, curx, cury, movx, movy, tag);
                curx = movx;
                cury = movy;
                open = false;
            }
            pi.next();
        }
        if (open) {
            // A subpath with no closePath is closed all the same: the enclosed area is the closed
            // path's. It is what Shape's spec says and what the JDK does.
            appendLine(out, curx, cury, movx, movy, tag);
        }
    }

    static void appendLine(ArrayList<AreaCurve> out, double x0, double y0,
                           double x1, double y1, int tag) {
        if (y0 == y1) {
            return;
        }
        double[] c = new double[4];
        c[0] = x0;
        c[1] = y0;
        c[2] = x1;
        c[3] = y1;
        appendMonotone(out, c, 1, tag);
    }

    static void appendQuad(ArrayList<AreaCurve> out, double x0, double y0,
                           double cx, double cy, double x1, double y1, int tag) {
        double[] c = new double[6];
        c[0] = x0;
        c[1] = y0;
        c[2] = cx;
        c[3] = cy;
        c[4] = x1;
        c[5] = y1;
        // dy/dt = 2*[(cy-y0) + t*(y0 - 2cy + y1)]
        double a = y0 - 2.0 * cy + y1;
        double b = cy - y0;
        double[] roots = new double[2];
        int n = 0;
        if (a != 0.0) {
            double t = -b / a;
            if (t > 0.0 && t < 1.0) {
                roots[n] = t;
                n = n + 1;
            }
        }
        splitAndAppend(out, c, 2, roots, n, tag);
    }

    static void appendCubic(ArrayList<AreaCurve> out, double x0, double y0,
                            double cx1, double cy1, double cx2, double cy2,
                            double x1, double y1, int tag) {
        double[] c = new double[8];
        c[0] = x0;
        c[1] = y0;
        c[2] = cx1;
        c[3] = cy1;
        c[4] = cx2;
        c[5] = cy2;
        c[6] = x1;
        c[7] = y1;
        // dy/dt / 3 = p*t^2 + q*t + r with the forward differences of the Y.
        double d0 = cy1 - y0;
        double d1 = cy2 - cy1;
        double d2 = y1 - cy2;
        double p = d0 - 2.0 * d1 + d2;
        double q = 2.0 * (d1 - d0);
        double r = d0;
        double[] roots = new double[2];
        int n = 0;
        if (p == 0.0) {
            if (q != 0.0) {
                double t = -r / q;
                if (t > 0.0 && t < 1.0) {
                    roots[n] = t;
                    n = n + 1;
                }
            }
        } else {
            double disc = q * q - 4.0 * p * r;
            if (disc >= 0.0) {
                double sq = Math.sqrt(disc);
                double ta = (-q - sq) / (2.0 * p);
                double tb = (-q + sq) / (2.0 * p);
                if (ta > tb) {
                    double tmp = ta;
                    ta = tb;
                    tb = tmp;
                }
                if (ta > 0.0 && ta < 1.0) {
                    roots[n] = ta;
                    n = n + 1;
                }
                if (tb > 0.0 && tb < 1.0 && (n == 0 || tb > roots[0])) {
                    roots[n] = tb;
                    n = n + 1;
                }
            }
        }
        splitAndAppend(out, c, 3, roots, n, tag);
    }

    private static void splitAndAppend(ArrayList<AreaCurve> out, double[] c, int n,
                                       double[] roots, int count, int tag) {
        double t0 = 0.0;
        int i = 0;
        while (i <= count) {
            double t1 = 1.0;
            if (i < count) {
                t1 = roots[i];
            }
            if (t1 > t0) {
                appendMonotone(out, subCurve(c, n, t0, t1), n, tag);
            }
            t0 = t1;
            i = i + 1;
        }
    }

    // It stores the piece already monotonic, turned top to bottom. The interior control points are
    // clamped to the Y range: mathematically they are inside already after cutting at the extrema,
    // and the clamping only takes out the floating-point noise that would ruin `xForY`'s
    // bisection.
    static void appendMonotone(ArrayList<AreaCurve> out, double[] c, int n, int tag) {
        double y0 = c[1];
        double y1 = c[2 * n + 1];
        if (y0 == y1) {
            return;
        }
        int dir = 1;
        if (y0 > y1) {
            c = reverse(c, n);
            dir = -1;
        }
        double top = c[1];
        double bot = c[2 * n + 1];
        int i = 1;
        while (i < n) {
            if (c[2 * i + 1] < top) {
                c[2 * i + 1] = top;
            }
            if (c[2 * i + 1] > bot) {
                c[2 * i + 1] = bot;
            }
            i = i + 1;
        }
        out.add(new AreaCurve(n, c, dir, tag));
    }

    static double[] reverse(double[] c, int n) {
        double[] r = new double[2 * (n + 1)];
        int i = 0;
        while (i <= n) {
            r[2 * i] = c[2 * (n - i)];
            r[2 * i + 1] = c[2 * (n - i) + 1];
            i = i + 1;
        }
        return r;
    }

    // --- intersections ---------------------------------------------------------------------------

    /**
     * The (ta, tb) pairs where the two curves touch, added to `out`.
     *
     * Line against line is solved in closed form, the collinear case included --two abutting edges
     * is the normal thing, not the odd one: it happens as soon as two rectangles sharing a side are
     * united, and the general subdivision method runs away to infinity there. The other pairs are
     * solved by subdividing the pair and discarding by bounding boxes, which is exact as far as the
     * double reaches and does not depend on solving degree-6 polynomials.
     */
    static void intersections(AreaCurve a, AreaCurve b, ArrayList<double[]> out) {
        if (a.ytop() > b.ybot() || b.ytop() > a.ybot()) {
            return;
        }
        if (sameGeometry(a, b)) {
            // Coincident tip to tip: there is nothing to cut, AreaOp groups them.
            return;
        }
        if (a.order == 1 && b.order == 1) {
            lineLine(a, b, out);
            return;
        }
        subdivide(a.c, a.order, 0.0, 1.0, b.c, b.order, 0.0, 1.0, 0, out, scale(a, b));
    }

    static boolean sameGeometry(AreaCurve a, AreaCurve b) {
        if (a.order != b.order) {
            return false;
        }
        int i = 0;
        while (i < a.c.length) {
            if (a.c[i] != b.c[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static double scale(AreaCurve a, AreaCurve b) {
        double m = 1.0;
        int i = 0;
        while (i < a.c.length) {
            double v = Math.abs(a.c[i]);
            if (v > m) {
                m = v;
            }
            i = i + 1;
        }
        i = 0;
        while (i < b.c.length) {
            double v = Math.abs(b.c[i]);
            if (v > m) {
                m = v;
            }
            i = i + 1;
        }
        return m;
    }

    private static void lineLine(AreaCurve a, AreaCurve b, ArrayList<double[]> out) {
        double sc = scale(a, b);
        double tol = 1.0e-11 * sc;
        double ax = a.c[0];
        double ay = a.c[1];
        double adx = a.c[2] - ax;
        double ady = a.c[3] - ay;
        double bx = b.c[0];
        double by = b.c[1];
        double bdx = b.c[2] - bx;
        double bdy = b.c[3] - by;
        double den = adx * bdy - ady * bdx;
        double rx = bx - ax;
        double ry = by - ay;
        double len = Math.sqrt(adx * adx + ady * ady);
        if (Math.abs(den) <= tol * len) {
            // Parallel. They only matter if they are collinear as well and overlap.
            double perp = Math.abs(rx * ady - ry * adx);
            if (len == 0.0 || perp > tol * len) {
                return;
            }
            double y0 = Math.max(a.ytop(), b.ytop());
            double y1 = Math.min(a.ybot(), b.ybot());
            if (y1 <= y0) {
                return;
            }
            addPair(out, (y0 - a.ytop()) / (a.ybot() - a.ytop()),
                    (y0 - b.ytop()) / (b.ybot() - b.ytop()));
            addPair(out, (y1 - a.ytop()) / (a.ybot() - a.ytop()),
                    (y1 - b.ytop()) / (b.ybot() - b.ytop()));
            return;
        }
        double ta = (rx * bdy - ry * bdx) / den;
        double tb = (rx * ady - ry * adx) / den;
        if (ta < 0.0 || ta > 1.0 || tb < 0.0 || tb > 1.0) {
            return;
        }
        addPair(out, ta, tb);
    }

    private static void addPair(ArrayList<double[]> out, double ta, double tb) {
        if (ta < 0.0) {
            ta = 0.0;
        }
        if (ta > 1.0) {
            ta = 1.0;
        }
        if (tb < 0.0) {
            tb = 0.0;
        }
        if (tb > 1.0) {
            tb = 1.0;
        }
        double[] p = new double[2];
        p[0] = ta;
        p[1] = tb;
        out.add(p);
    }

    private static void subdivide(double[] ca, int na, double ta0, double ta1,
                                  double[] cb, int nb, double tb0, double tb1,
                                  int depth, ArrayList<double[]> out, double sc) {
        if (out.size() > 64) {
            return;
        }
        double[] ba = bbox(ca, na);
        double[] bb = bbox(cb, nb);
        double tol = 1.0e-12 * sc;
        if (ba[2] < bb[0] - tol || bb[2] < ba[0] - tol
                || ba[3] < bb[1] - tol || bb[3] < ba[1] - tol) {
            return;
        }
        double da = Math.max(ba[2] - ba[0], ba[3] - ba[1]);
        double db = Math.max(bb[2] - bb[0], bb[3] - bb[1]);
        if (depth >= 60 || (da <= tol && db <= tol)) {
            addPair(out, (ta0 + ta1) * 0.5, (tb0 + tb1) * 0.5);
            return;
        }
        if (da >= db) {
            double tm = (ta0 + ta1) * 0.5;
            subdivide(leftPart(ca, na, 0.5), na, ta0, tm, cb, nb, tb0, tb1, depth + 1, out, sc);
            subdivide(rightPart(ca, na, 0.5), na, tm, ta1, cb, nb, tb0, tb1, depth + 1, out, sc);
        } else {
            double tm = (tb0 + tb1) * 0.5;
            subdivide(ca, na, ta0, ta1, leftPart(cb, nb, 0.5), nb, tb0, tm, depth + 1, out, sc);
            subdivide(ca, na, ta0, ta1, rightPart(cb, nb, 0.5), nb, tm, tb1, depth + 1, out, sc);
        }
    }

    /** {minx, miny, maxx, maxy} of the control points' hull. */
    static double[] bbox(double[] c, int n) {
        double x0 = c[0];
        double y0 = c[1];
        double x1 = c[0];
        double y1 = c[1];
        int i = 1;
        while (i <= n) {
            double x = c[2 * i];
            double y = c[2 * i + 1];
            if (x < x0) {
                x0 = x;
            }
            if (x > x1) {
                x1 = x;
            }
            if (y < y0) {
                y0 = y;
            }
            if (y > y1) {
                y1 = y;
            }
            i = i + 1;
        }
        double[] r = new double[4];
        r[0] = x0;
        r[1] = y0;
        r[2] = x1;
        r[3] = y1;
        return r;
    }
}
