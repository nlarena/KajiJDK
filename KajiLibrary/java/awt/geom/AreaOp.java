package java.awt.geom;

import java.util.ArrayList;
import java.util.HashMap;

// The engine of Area's boolean operations (not API).
//
// The classic way of settling this --the JDK's-- is a sweep by scan lines that classifies the active
// edges as it goes down. It is fast and it is an inferno of edge cases: the active list's order has
// to be kept while two curves cross, and every numeric tie is paid for in the result.
//
// Here it is done the other way round, in three independent passes, and on purpose:
//
//   1. **Split.** Every curve is cut against every other at their intersections. When that is over,
//      no curve passes through another's interior: they only touch at the tips. Quadratic in the
//      number of pieces, and that is where the simplicity of what follows is paid for.
//
//   2. **Classify.** Each piece is looked at **on its own**: it is evaluated at its midpoint
//      --which, by the above, is nobody's tip and is crossed by nobody-- and each operand's winding
//      number is counted by casting a ray towards -X. That tells whether the operand is inside just
//      to the left and just to the right of the piece; the boolean operation is applied to both
//      sides; and the piece survives if and only if the two sides come out different. There is no
//      state shared between pieces, so a tie settled wrongly in one does not contaminate the rest.
//
//   3. **Close.** The horizontal stretches were discarded when the pieces were built, so the loops
//      are left with horizontal holes. They are remade with pass 2's criterion, but asking above and
//      below instead of left and right, at every Y where some surviving piece has a tip.
//
// Two exactly coincident curves --two rectangles sharing a side, or the XOR of a shape with itself--
// are not cut against each other: they are **grouped**, and the group carries the sum of the
// directions and each operand's count of pieces. That is what makes the abutting case work without
// epsilons: the two edges are a single geometric object with contribution +2, or +1 and -1, or
// whatever it may be.
//
// The result's orientation is "the interior ends up to the right of the piece when it is walked top
// to bottom", which gives interior points winding +1 with the non-zero rule. It is one of the two
// valid orientations and does not necessarily agree with the one the JDK chooses; the fill rule is
// non-zero in both cases, so the region is the same.
final class AreaOp {

    static final int ADD = 0;
    static final int SUB = 1;
    static final int INT = 2;
    static final int XOR = 3;

    private AreaOp() {
    }

    // One geometric object of the already split plane: the coincident pieces all fall into the
    // same one.
    private static final class Group {
        AreaCurve rep;
        int dirL;
        int cntL;
        int dirR;
        int cntR;
        int keep;
    }

    // One edge of the result, with the control points already in the order it is walked in.
    private static final class Edge {
        int order;
        double[] c;
        boolean used;
    }

    /**
     * The `op` operation between the pieces marked LEFT (with rule `ruleL`) and those marked RIGHT
     * (with rule `ruleR`), as a closed path with the non-zero rule.
     */
    static Path2D compute(ArrayList<AreaCurve> curves, int ruleL, int ruleR, int op) {
        Path2D path = Path2D.newDouble(PathIterator.WIND_NON_ZERO);
        ArrayList<AreaCurve> pieces = split(curves);
        ArrayList<Group> groups = group(pieces);
        classify(groups, ruleL, ruleR, op);
        ArrayList<Edge> edges = new ArrayList<Edge>();
        int i = 0;
        while (i < groups.size()) {
            Group g = groups.get(i);
            if (g.keep != 0) {
                edges.add(edgeFromCurve(g.rep, g.keep));
            }
            i = i + 1;
        }
        horizontals(groups, ruleL, ruleR, op, edges);
        chain(edges, path);
        return path;
    }

    // --- pass 1: split ---------------------------------------------------------------------------

    private static ArrayList<AreaCurve> split(ArrayList<AreaCurve> curves) {
        int n = curves.size();
        ArrayList<ArrayList<double[]>> cuts = new ArrayList<ArrayList<double[]>>();
        int i = 0;
        while (i < n) {
            cuts.add(new ArrayList<double[]>());
            i = i + 1;
        }
        ArrayList<double[]> hits = new ArrayList<double[]>();
        i = 0;
        while (i < n) {
            AreaCurve a = curves.get(i);
            int j = i + 1;
            while (j < n) {
                AreaCurve b = curves.get(j);
                hits.clear();
                AreaCurve.intersections(a, b, hits);
                int k = 0;
                while (k < hits.size()) {
                    double[] pair = hits.get(k);
                    recordCut(a, pair[0], b, pair[1], cuts.get(i), cuts.get(j));
                    k = k + 1;
                }
                j = j + 1;
            }
            i = i + 1;
        }
        ArrayList<AreaCurve> pieces = new ArrayList<AreaCurve>();
        i = 0;
        while (i < n) {
            sliceCurve(curves.get(i), cuts.get(i), pieces);
            i = i + 1;
        }
        return pieces;
    }

    // The cut point is chosen once only for both curves: if it falls on a tip that tip is used as
    // it stands (that way the neighbouring piece, which is not split, stays joined by exact
    // equality) and if not, the average of the two evaluations. Without this agreement the two
    // pieces would end at points differing by an ulp and the chaining would not join them.
    private static void recordCut(AreaCurve a, double ta, AreaCurve b, double tb,
                                  ArrayList<double[]> cutsA, ArrayList<double[]> cutsB) {
        double tol = 1.0e-9 * scaleOf(a, b);
        double[] pa = a.point(ta);
        double[] pb = b.point(tb);
        boolean aHead = near(pa, a.xtop(), a.ytop(), tol);
        boolean aTail = near(pa, a.xbot(), a.ybot(), tol);
        boolean bHead = near(pb, b.xtop(), b.ytop(), tol);
        boolean bTail = near(pb, b.xbot(), b.ybot(), tol);
        double px;
        double py;
        if (aHead) {
            px = a.xtop();
            py = a.ytop();
        } else if (aTail) {
            px = a.xbot();
            py = a.ybot();
        } else if (bHead) {
            px = b.xtop();
            py = b.ytop();
        } else if (bTail) {
            px = b.xbot();
            py = b.ybot();
        } else {
            px = (pa[0] + pb[0]) * 0.5;
            py = (pa[1] + pb[1]) * 0.5;
        }
        if (!aHead && !aTail) {
            cutsA.add(cutEntry(ta, px, py));
        }
        if (!bHead && !bTail) {
            cutsB.add(cutEntry(tb, px, py));
        }
    }

    private static double[] cutEntry(double t, double px, double py) {
        double[] e = new double[3];
        e[0] = t;
        e[1] = px;
        e[2] = py;
        return e;
    }

    private static boolean near(double[] p, double x, double y, double tol) {
        return Math.abs(p[0] - x) <= tol && Math.abs(p[1] - y) <= tol;
    }

    private static double scaleOf(AreaCurve a, AreaCurve b) {
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

    private static void sliceCurve(AreaCurve a, ArrayList<double[]> cuts,
                                   ArrayList<AreaCurve> out) {
        sortByFirst(cuts);
        int n = a.order;
        double tPrev = 0.0;
        double pxPrev = a.c[0];
        double pyPrev = a.c[1];
        int k = 0;
        while (k <= cuts.size()) {
            double t;
            double px;
            double py;
            if (k < cuts.size()) {
                double[] e = cuts.get(k);
                t = e[0];
                px = e[1];
                py = e[2];
            } else {
                t = 1.0;
                px = a.c[2 * n];
                py = a.c[2 * n + 1];
            }
            if (t > tPrev + 1.0e-13 || k == cuts.size()) {
                if (t > tPrev) {
                    double[] cc = AreaCurve.subCurve(a.c, n, tPrev, t);
                    cc[0] = pxPrev;
                    cc[1] = pyPrev;
                    cc[2 * n] = px;
                    cc[2 * n + 1] = py;
                    addPiece(out, cc, n, a.dir, a.tag);
                }
                tPrev = t;
                pxPrev = px;
                pyPrev = py;
            }
            k = k + 1;
        }
    }

    private static void addPiece(ArrayList<AreaCurve> out, double[] c, int n, int dir, int tag) {
        double top = c[1];
        double bot = c[2 * n + 1];
        if (top >= bot) {
            return;
        }
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

    private static void sortByFirst(ArrayList<double[]> l) {
        int i = 1;
        while (i < l.size()) {
            double[] v = l.get(i);
            int j = i - 1;
            while (j >= 0 && l.get(j)[0] > v[0]) {
                l.set(j + 1, l.get(j));
                j = j - 1;
            }
            l.set(j + 1, v);
            i = i + 1;
        }
    }

    // --- pass 2: group and classify --------------------------------------------------------------

    private static ArrayList<Group> group(ArrayList<AreaCurve> pieces) {
        HashMap<String, Group> byKey = new HashMap<String, Group>();
        ArrayList<Group> groups = new ArrayList<Group>();
        int i = 0;
        while (i < pieces.size()) {
            AreaCurve p = pieces.get(i);
            String k = curveKey(p);
            Group g = byKey.get(k);
            if (g == null) {
                g = new Group();
                g.rep = p;
                byKey.put(k, g);
                groups.add(g);
            }
            if (p.tag == AreaCurve.LEFT) {
                g.dirL = g.dirL + p.dir;
                g.cntL = g.cntL + 1;
            } else {
                g.dirR = g.dirR + p.dir;
                g.cntR = g.cntR + 1;
            }
            i = i + 1;
        }
        return groups;
    }

    private static String curveKey(AreaCurve c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.order);
        int i = 0;
        while (i < c.c.length) {
            sb.append(':');
            sb.append(bits(c.c[i]));
            i = i + 1;
        }
        return sb.toString();
    }

    private static long bits(double v) {
        return java.lang.Double.doubleToLongBits(v + 0.0);
    }

    private static void classify(ArrayList<Group> groups, int ruleL, int ruleR, int op) {
        int n = groups.size();
        int i = 0;
        while (i < n) {
            Group g = groups.get(i);
            double[] m = g.rep.point(0.5);
            int wL = 0;
            int cL = 0;
            int wR = 0;
            int cR = 0;
            int j = 0;
            while (j < n) {
                if (j != i) {
                    Group h = groups.get(j);
                    if (h.rep.ytop() <= m[1] && m[1] < h.rep.ybot()
                            && h.rep.xForY(m[1]) < m[0]) {
                        wL = wL + h.dirL;
                        cL = cL + h.cntL;
                        wR = wR + h.dirR;
                        cR = cR + h.cntR;
                    }
                }
                j = j + 1;
            }
            boolean left = apply(op, inside(wL, cL, ruleL), inside(wR, cR, ruleR));
            boolean right = apply(op, inside(wL + g.dirL, cL + g.cntL, ruleL),
                    inside(wR + g.dirR, cR + g.cntR, ruleR));
            if (left == right) {
                g.keep = 0;
            } else if (right) {
                g.keep = 1;
            } else {
                g.keep = -1;
            }
            i = i + 1;
        }
    }

    static boolean inside(int w, int count, int rule) {
        if (rule == PathIterator.WIND_EVEN_ODD) {
            return (count & 1) != 0;
        }
        return w != 0;
    }

    static boolean apply(int op, boolean a, boolean b) {
        if (op == ADD) {
            return a || b;
        }
        if (op == SUB) {
            return a && !b;
        }
        if (op == INT) {
            return a && b;
        }
        return a != b;
    }

    // --- pass 3: the horizontal stretches --------------------------------------------------------

    private static void horizontals(ArrayList<Group> groups, int ruleL, int ruleR, int op,
                                    ArrayList<Edge> out) {
        ArrayList<double[]> ys = new ArrayList<double[]>();
        int i = 0;
        while (i < groups.size()) {
            Group g = groups.get(i);
            if (g.keep != 0) {
                addUnique(ys, g.rep.ytop());
                addUnique(ys, g.rep.ybot());
            }
            i = i + 1;
        }
        sortByFirst(ys);
        i = 0;
        while (i < ys.size()) {
            horizontalsAt(groups, ys.get(i)[0], ruleL, ruleR, op, out);
            i = i + 1;
        }
    }

    private static void addUnique(ArrayList<double[]> l, double v) {
        int i = 0;
        while (i < l.size()) {
            if (l.get(i)[0] == v) {
                return;
            }
            i = i + 1;
        }
        double[] e = new double[1];
        e[0] = v;
        l.add(e);
    }

    private static void horizontalsAt(ArrayList<Group> groups, double y, int ruleL, int ruleR,
                                      int op, ArrayList<Edge> out) {
        // The Xs where the result's edge cuts this height. Between two consecutive cuts the
        // "inside" does not change, so asking in the middle is enough.
        ArrayList<double[]> xs = new ArrayList<double[]>();
        int i = 0;
        while (i < groups.size()) {
            Group g = groups.get(i);
            if (g.keep != 0 && g.rep.ytop() <= y && y <= g.rep.ybot()) {
                addUnique(xs, g.rep.xForY(y));
            }
            i = i + 1;
        }
        sortByFirst(xs);
        i = 0;
        while (i + 1 < xs.size()) {
            double x0 = xs.get(i)[0];
            double x1 = xs.get(i + 1)[0];
            double mx = (x0 + x1) * 0.5;
            boolean above = horizontalSide(groups, mx, y, true, ruleL, ruleR, op);
            boolean below = horizontalSide(groups, mx, y, false, ruleL, ruleR, op);
            if (above != below) {
                double[] c = new double[4];
                if (above) {
                    c[0] = x0;
                    c[1] = y;
                    c[2] = x1;
                    c[3] = y;
                } else {
                    c[0] = x1;
                    c[1] = y;
                    c[2] = x0;
                    c[3] = y;
                }
                Edge e = new Edge();
                e.order = 1;
                e.c = c;
                out.add(e);
            }
            i = i + 1;
        }
    }

    // The result just above (or just below) (mx, y). "Just above" means the groups that still
    // exist at y-delta: those beginning before `y` and not ended before reaching it.
    private static boolean horizontalSide(ArrayList<Group> groups, double mx, double y,
                                          boolean above, int ruleL, int ruleR, int op) {
        int wL = 0;
        int cL = 0;
        int wR = 0;
        int cR = 0;
        int i = 0;
        while (i < groups.size()) {
            Group h = groups.get(i);
            boolean crosses;
            if (above) {
                crosses = h.rep.ytop() < y && h.rep.ybot() >= y;
            } else {
                crosses = h.rep.ytop() <= y && h.rep.ybot() > y;
            }
            if (crosses && h.rep.xForY(y) < mx) {
                wL = wL + h.dirL;
                cL = cL + h.cntL;
                wR = wR + h.dirR;
                cR = cR + h.cntR;
            }
            i = i + 1;
        }
        return apply(op, inside(wL, cL, ruleL), inside(wR, cR, ruleR));
    }

    // --- closing: building the loops -------------------------------------------------------------

    private static Edge edgeFromCurve(AreaCurve c, int keep) {
        Edge e = new Edge();
        e.order = c.order;
        if (keep > 0) {
            e.c = AreaCurve.copyOf(c.c);
        } else {
            e.c = AreaCurve.reverse(c.c, c.order);
        }
        return e;
    }

    private static void chain(ArrayList<Edge> edges, Path2D path) {
        HashMap<String, ArrayList<Edge>> byStart = new HashMap<String, ArrayList<Edge>>();
        int i = 0;
        while (i < edges.size()) {
            Edge e = edges.get(i);
            String k = pointKey(e.c[0], e.c[1]);
            ArrayList<Edge> l = byStart.get(k);
            if (l == null) {
                l = new ArrayList<Edge>();
                byStart.put(k, l);
            }
            l.add(e);
            i = i + 1;
        }
        int limit = edges.size() + 1;
        i = 0;
        while (i < edges.size()) {
            Edge e = edges.get(i);
            if (!e.used) {
                path.moveTo(e.c[0], e.c[1]);
                Edge current = e;
                int steps = 0;
                while (current != null && steps < limit) {
                    current.used = true;
                    Edge next = nextEdge(byStart, current);
                    // A loop's last straight stretch is drawn by `closePath`: emitting it as well
                    // would leave a redundant `lineTo` to the starting point in every subpath.
                    boolean lastLine = next == null && current.order == 1
                            && current.c[2] == e.c[0] && current.c[3] == e.c[1];
                    if (!lastLine) {
                        emit(path, current);
                    }
                    current = next;
                    steps = steps + 1;
                }
                path.closePath();
            }
            i = i + 1;
        }
    }

    private static Edge nextEdge(HashMap<String, ArrayList<Edge>> byStart, Edge current) {
        int n = current.order;
        ArrayList<Edge> l = byStart.get(pointKey(current.c[2 * n], current.c[2 * n + 1]));
        if (l == null) {
            return null;
        }
        int i = 0;
        while (i < l.size()) {
            Edge e = l.get(i);
            if (!e.used) {
                return e;
            }
            i = i + 1;
        }
        return null;
    }

    private static void emit(Path2D p, Edge e) {
        if (e.order == 1) {
            p.lineTo(e.c[2], e.c[3]);
        } else if (e.order == 2) {
            p.quadTo(e.c[2], e.c[3], e.c[4], e.c[5]);
        } else {
            p.curveTo(e.c[2], e.c[3], e.c[4], e.c[5], e.c[6], e.c[7]);
        }
    }

    private static String pointKey(double x, double y) {
        StringBuilder sb = new StringBuilder();
        sb.append(bits(x));
        sb.append(',');
        sb.append(bits(y));
        return sb.toString();
    }
}
