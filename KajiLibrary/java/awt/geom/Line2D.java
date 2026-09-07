package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;

// KajiLibrary's java.awt.geom.Line2D -- a line segment. The surface is complete.
//
// `linesIntersect` is one of the package's most misused methods, so it is worth saying exactly what
// it does: it returns true if the two **segments** (not the infinite lines) have some point in
// common, touching at an end included, and also when they are collinear and overlap. The test is the
// one of the four crossed `relativeCCW`s; the collinear case comes free because `relativeCCW` does
// not return 0 for a collinear point falling **outside** the segment, but ±1.
//
// A Line2D encloses no area, so its four `contains` always return false. It is no shortcut: a
// segment contains no point in Shape's sense of "insideness".
public abstract class Line2D implements Shape, Cloneable {

    // A segment with float coordinates.
    public static class Float extends Line2D implements java.io.Serializable {

        public float x1;
        public float y1;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float x2, float y2) {
            setLine(x1, y1, x2, y2);
        }

        public Float(Point2D p1, Point2D p2) {
            setLine(p1, p2);
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

        public double getX2() {
            return (double) this.x2;
        }

        public double getY2() {
            return (double) this.y2;
        }

        public Point2D getP2() {
            return Point2D.newFloat(this.x2, this.y2);
        }

        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        public void setLine(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Rectangle2D getBounds2D() {
            float x;
            float y;
            float w;
            float h;
            if (this.x1 < this.x2) {
                x = this.x1;
                w = this.x2 - this.x1;
            } else {
                x = this.x2;
                w = this.x1 - this.x2;
            }
            if (this.y1 < this.y2) {
                y = this.y1;
                h = this.y2 - this.y1;
            } else {
                y = this.y2;
                h = this.y1 - this.y2;
            }
            return Rectangle2D.newFloat(x, y, w, h);
        }
    }

    // A segment with double coordinates.
    public static class Double extends Line2D implements java.io.Serializable {

        public double x1;
        public double y1;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double x2, double y2) {
            setLine(x1, y1, x2, y2);
        }

        public Double(Point2D p1, Point2D p2) {
            setLine(p1, p2);
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

        public double getX2() {
            return this.x2;
        }

        public double getY2() {
            return this.y2;
        }

        public Point2D getP2() {
            return Point2D.newDouble(this.x2, this.y2);
        }

        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Rectangle2D getBounds2D() {
            double x;
            double y;
            double w;
            double h;
            if (this.x1 < this.x2) {
                x = this.x1;
                w = this.x2 - this.x1;
            } else {
                x = this.x2;
                w = this.x1 - this.x2;
            }
            if (this.y1 < this.y2) {
                y = this.y1;
                h = this.y2 - this.y1;
            } else {
                y = this.y2;
                h = this.y1 - this.y2;
            }
            return Rectangle2D.newDouble(x, y, w, h);
        }
    }

    protected Line2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract Point2D getP1();

    public abstract double getX2();

    public abstract double getY2();

    public abstract Point2D getP2();

    public abstract void setLine(double x1, double y1, double x2, double y2);

    public void setLine(Point2D p1, Point2D p2) {
        setLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public void setLine(Line2D l) {
        setLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    // It returns -1, 0 or 1 according to which side of the directed line (x1,y1)->(x2,y2) the
    // point falls on. The 0 is reserved for the collinear points falling **inside** the segment: a
    // collinear one past the end returns ±1 according to which tip it went past. That distinction is
    // what makes `linesIntersect` get the collinear cases right without treating them apart.
    public static int relativeCCW(double x1, double y1, double x2, double y2,
                                  double px, double py) {
        x2 = x2 - x1;
        y2 = y2 - y1;
        px = px - x1;
        py = py - y1;
        double ccw = px * y2 - py * x2;
        if (ccw == 0.0) {
            // Collinear: it is classified by the projection onto the segment.
            ccw = px * x2 + py * y2;
            if (ccw > 0.0) {
                // The reckoning is repeated relative to the other end. x2,y2 are already negated
                // with respect to that origin, so shifting px,py is enough.
                px = px - x2;
                py = py - y2;
                ccw = px * x2 + py * y2;
                if (ccw < 0.0) {
                    ccw = 0.0;
                }
            }
        }
        if (ccw < 0.0) {
            return -1;
        }
        if (ccw > 0.0) {
            return 1;
        }
        return 0;
    }

    public int relativeCCW(double px, double py) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public int relativeCCW(Point2D p) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(), p.getX(), p.getY());
    }

    public static boolean linesIntersect(double x1, double y1, double x2, double y2,
                                         double x3, double y3, double x4, double y4) {
        return ((relativeCCW(x1, y1, x2, y2, x3, y3) * relativeCCW(x1, y1, x2, y2, x4, y4) <= 0)
                && (relativeCCW(x3, y3, x4, y4, x1, y1)
                    * relativeCCW(x3, y3, x4, y4, x2, y2) <= 0));
    }

    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        return linesIntersect(x1, y1, x2, y2, getX1(), getY1(), getX2(), getY2());
    }

    public boolean intersectsLine(Line2D l) {
        return linesIntersect(l.getX1(), l.getY1(), l.getX2(), l.getY2(),
                              getX1(), getY1(), getX2(), getY2());
    }

    // Distance to the **segment**: if the projection falls outside, the distance is to the nearest
    // tip. With a degenerate segment (both ends equal) it gives the distance to the point, which is
    // right; the infinite-line version, on the other hand, returns NaN there -- a line that does not
    // exist has no defined distance, and the JDK does not invent one either.
    public static double ptSegDistSq(double x1, double y1, double x2, double y2,
                                     double px, double py) {
        x2 = x2 - x1;
        y2 = y2 - y1;
        px = px - x1;
        py = py - y1;
        double dotprod = px * x2 + py * y2;
        double projlenSq;
        if (dotprod <= 0.0) {
            // The point falls on (x1,y1)'s side: the clamped projection measures 0.
            projlenSq = 0.0;
        } else {
            // It moves to vectors measured from (x2,y2).
            px = x2 - px;
            py = y2 - py;
            dotprod = px * x2 + py * y2;
            if (dotprod <= 0.0) {
                projlenSq = 0.0;
            } else {
                projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
            }
        }
        double lenSq = px * px + py * py - projlenSq;
        if (lenSq < 0) {
            lenSq = 0;
        }
        return lenSq;
    }

    public static double ptSegDist(double x1, double y1, double x2, double y2,
                                   double px, double py) {
        return Math.sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
    }

    public double ptSegDistSq(double px, double py) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptSegDistSq(Point2D pt) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    public double ptSegDist(double px, double py) {
        return ptSegDist(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptSegDist(Point2D pt) {
        return ptSegDist(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    public static double ptLineDistSq(double x1, double y1, double x2, double y2,
                                      double px, double py) {
        x2 = x2 - x1;
        y2 = y2 - y1;
        px = px - x1;
        py = py - y1;
        double dotprod = px * x2 + py * y2;
        double projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
        double lenSq = px * px + py * py - projlenSq;
        if (lenSq < 0) {
            lenSq = 0;
        }
        return lenSq;
    }

    public static double ptLineDist(double x1, double y1, double x2, double y2,
                                    double px, double py) {
        return Math.sqrt(ptLineDistSq(x1, y1, x2, y2, px, py));
    }

    public double ptLineDistSq(double px, double py) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptLineDistSq(Point2D pt) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    public double ptLineDist(double px, double py) {
        return ptLineDist(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptLineDist(Point2D pt) {
        return ptLineDist(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    // A segment encloses no area: it never contains anything.
    public boolean contains(double x, double y) {
        return false;
    }

    public boolean contains(Point2D p) {
        return false;
    }

    public boolean contains(double x, double y, double w, double h) {
        return false;
    }

    public boolean contains(Rectangle2D r) {
        return false;
    }

    public boolean intersects(double x, double y, double w, double h) {
        return intersects(Rectangle2D.newDouble(x, y, w, h));
    }

    public boolean intersects(Rectangle2D r) {
        return r.intersectsLine(getX1(), getY1(), getX2(), getY2());
    }

    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new LineIterator(this, at);
    }

    // A segment is flat already: the flattening parameter changes nothing.
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new LineIterator(this, at);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
}
