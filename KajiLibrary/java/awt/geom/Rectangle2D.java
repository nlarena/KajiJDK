package java.awt.geom;

// KajiLibrary's java.awt.geom.Rectangle2D -- an axis-aligned rectangle. The surface is complete.
//
// Two details that tend to be implemented wrongly and that are here on purpose:
//
//   * `intersect(a, b, dest)` **does not normalize**: if the rectangles do not touch, the
//     destination is left with a negative width and/or height (x=5, w=-4, say). That is what the JDK
//     does and it is useful -- `isEmpty()` recognizes it as empty -- but it tempts one into "fixing"
//     it with setFrameFromDiagonal, which would normalize it and give another rectangle.
//
//   * `intersectsLine` clips the segment against the frame with Cohen-Sutherland's outcode
//     algorithm instead of testing the four edges. It is the package's most misused method: testing
//     "does some edge cut the segment?" gives false when the segment is entirely inside.
//
// At the end of the file there are internal `newDouble`/`newFloat` factories. They exist for the
// same reason as Point2D's: this house's javac does not properly resolve two types with the same
// simple name in one compilation unit, and `Arc2D`, `Ellipse2D`, `Line2D`, `Path2D` and
// `RoundRectangle2D` have their **own** nested `Double` and `Float` classes, so they cannot also
// name the ones from here. See the long note in Point2D.java's header.
public abstract class Rectangle2D extends RectangularShape {

    /** The point is left of the rectangle. */
    public static final int OUT_LEFT = 1;

    /** The point is above the rectangle. */
    public static final int OUT_TOP = 2;

    /** The point is right of the rectangle. */
    public static final int OUT_RIGHT = 4;

    /** The point is below the rectangle. */
    public static final int OUT_BOTTOM = 8;

    // A rectangle with float coordinates.
    public static class Float extends Rectangle2D implements java.io.Serializable {

        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
        }

        public Float(float x, float y, float w, float h) {
            setRect(x, y, w, h);
        }

        public double getX() {
            return (double) this.x;
        }

        public double getY() {
            return (double) this.y;
        }

        public double getWidth() {
            return (double) this.width;
        }

        public double getHeight() {
            return (double) this.height;
        }

        public boolean isEmpty() {
            return (this.width <= 0.0f) || (this.height <= 0.0f);
        }

        public void setRect(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        public void setRect(double x, double y, double w, double h) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
        }

        public void setRect(Rectangle2D r) {
            this.x = (float) r.getX();
            this.y = (float) r.getY();
            this.width = (float) r.getWidth();
            this.height = (float) r.getHeight();
        }

        public int outcode(double x, double y) {
            int out = 0;
            if (this.width <= 0.0f) {
                out = out | OUT_LEFT | OUT_RIGHT;
            } else if (x < (double) this.x) {
                out = out | OUT_LEFT;
            } else if (x > (double) this.x + (double) this.width) {
                out = out | OUT_RIGHT;
            }
            if (this.height <= 0.0f) {
                out = out | OUT_TOP | OUT_BOTTOM;
            } else if (y < (double) this.y) {
                out = out | OUT_TOP;
            } else if (y > (double) this.y + (double) this.height) {
                out = out | OUT_BOTTOM;
            }
            return out;
        }

        public Rectangle2D getBounds2D() {
            return new Float(this.x, this.y, this.width, this.height);
        }

        public Rectangle2D createIntersection(Rectangle2D r) {
            Rectangle2D dest;
            if (r instanceof Float) {
                dest = new Float();
            } else {
                dest = new Double();
            }
            Rectangle2D.intersect(this, r, dest);
            return dest;
        }

        public Rectangle2D createUnion(Rectangle2D r) {
            Rectangle2D dest;
            if (r instanceof Float) {
                dest = new Float();
            } else {
                dest = new Double();
            }
            Rectangle2D.union(this, r, dest);
            return dest;
        }

        public String toString() {
            return getClass().getName() + "[x=" + this.x + ",y=" + this.y
                    + ",w=" + this.width + ",h=" + this.height + "]";
        }
    }

    // A rectangle with double coordinates.
    public static class Double extends Rectangle2D implements java.io.Serializable {

        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double w, double h) {
            setRect(x, y, w, h);
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public double getWidth() {
            return this.width;
        }

        public double getHeight() {
            return this.height;
        }

        public boolean isEmpty() {
            return (this.width <= 0.0) || (this.height <= 0.0);
        }

        public void setRect(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        public void setRect(Rectangle2D r) {
            this.x = r.getX();
            this.y = r.getY();
            this.width = r.getWidth();
            this.height = r.getHeight();
        }

        public int outcode(double x, double y) {
            int out = 0;
            if (this.width <= 0.0) {
                out = out | OUT_LEFT | OUT_RIGHT;
            } else if (x < this.x) {
                out = out | OUT_LEFT;
            } else if (x > this.x + this.width) {
                out = out | OUT_RIGHT;
            }
            if (this.height <= 0.0) {
                out = out | OUT_TOP | OUT_BOTTOM;
            } else if (y < this.y) {
                out = out | OUT_TOP;
            } else if (y > this.y + this.height) {
                out = out | OUT_BOTTOM;
            }
            return out;
        }

        public Rectangle2D getBounds2D() {
            return new Double(this.x, this.y, this.width, this.height);
        }

        public Rectangle2D createIntersection(Rectangle2D r) {
            Rectangle2D dest = new Double();
            Rectangle2D.intersect(this, r, dest);
            return dest;
        }

        public Rectangle2D createUnion(Rectangle2D r) {
            Rectangle2D dest = new Double();
            Rectangle2D.union(this, r, dest);
            return dest;
        }

        public String toString() {
            return getClass().getName() + "[x=" + this.x + ",y=" + this.y
                    + ",w=" + this.width + ",h=" + this.height + "]";
        }
    }

    protected Rectangle2D() {
    }

    public abstract void setRect(double x, double y, double w, double h);

    public void setRect(Rectangle2D r) {
        setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public abstract int outcode(double x, double y);

    public int outcode(Point2D p) {
        return outcode(p.getX(), p.getY());
    }

    public abstract Rectangle2D createIntersection(Rectangle2D r);

    public abstract Rectangle2D createUnion(Rectangle2D r);

    public void setFrame(double x, double y, double w, double h) {
        setRect(x, y, w, h);
    }

    public Rectangle2D getBounds2D() {
        return (Rectangle2D) clone();
    }

    // A deliberate asymmetry (§ "insideness"): the left and top edges belong to the rectangle, the
    // right and bottom ones do not. That way two abutting rectangles share no points.
    public boolean contains(double x, double y) {
        double x0 = getX();
        double y0 = getY();
        return (x >= x0 && y >= y0 && x < x0 + getWidth() && y < y0 + getHeight());
    }

    public boolean intersects(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return (x + w > x0 && y + h > y0 && x < x0 + getWidth() && y < y0 + getHeight());
    }

    public boolean contains(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return (x >= x0 && y >= y0 && (x + w) <= x0 + getWidth() && (y + h) <= y0 + getHeight());
    }

    // Cohen-Sutherland: the end (x1,y1) is clipped against the edge its outcode points at until it
    // falls inside (return true) or until both ends end up on the same side of one and the same edge
    // (return false). Clipping --and not intersecting edges-- is what makes an entirely contained
    // segment return true.
    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        int out1;
        int out2 = outcode(x2, y2);
        if (out2 == 0) {
            return true;
        }
        out1 = outcode(x1, y1);
        while (out1 != 0) {
            if ((out1 & out2) != 0) {
                return false;
            }
            if ((out1 & (OUT_LEFT | OUT_RIGHT)) != 0) {
                double x = getX();
                if ((out1 & OUT_RIGHT) != 0) {
                    x = x + getWidth();
                }
                y1 = y1 + (x - x1) * (y2 - y1) / (x2 - x1);
                x1 = x;
            } else {
                double y = getY();
                if ((out1 & OUT_BOTTOM) != 0) {
                    y = y + getHeight();
                }
                x1 = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
                y1 = y;
            }
            out1 = outcode(x1, y1);
        }
        return true;
    }

    public boolean intersectsLine(Line2D l) {
        return intersectsLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    // Mind this: it does not normalize. If they do not intersect, `dest` is left with negative
    // dimensions.
    public static void intersect(Rectangle2D src1, Rectangle2D src2, Rectangle2D dest) {
        double x1 = Math.max(src1.getMinX(), src2.getMinX());
        double y1 = Math.max(src1.getMinY(), src2.getMinY());
        double x2 = Math.min(src1.getMaxX(), src2.getMaxX());
        double y2 = Math.min(src1.getMaxY(), src2.getMaxY());
        dest.setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    // Nor does it filter out the empty ones: the union of a zero-area rectangle with another
    // includes the degenerate point. It is what the JDK does.
    public static void union(Rectangle2D src1, Rectangle2D src2, Rectangle2D dest) {
        double x1 = Math.min(src1.getMinX(), src2.getMinX());
        double y1 = Math.min(src1.getMinY(), src2.getMinY());
        double x2 = Math.max(src1.getMaxX(), src2.getMaxX());
        double y2 = Math.max(src1.getMaxY(), src2.getMaxY());
        dest.setFrameFromDiagonal(x1, y1, x2, y2);
    }

    public void add(double newx, double newy) {
        double x1 = Math.min(getMinX(), newx);
        double x2 = Math.max(getMaxX(), newx);
        double y1 = Math.min(getMinY(), newy);
        double y2 = Math.max(getMaxY(), newy);
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    public void add(Point2D pt) {
        add(pt.getX(), pt.getY());
    }

    public void add(Rectangle2D r) {
        double x1 = Math.min(getMinX(), r.getMinX());
        double x2 = Math.max(getMaxX(), r.getMaxX());
        double y1 = Math.min(getMinY(), r.getMinY());
        double y2 = Math.max(getMaxY(), r.getMaxY());
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new RectIterator(this, at);
    }

    // A rectangle is flat already: flattening it changes nothing and the FlatteningPathIterator is
    // not needed.
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new RectIterator(this, at);
    }

    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits = bits + java.lang.Double.doubleToLongBits(getY()) * 37L;
        bits = bits + java.lang.Double.doubleToLongBits(getWidth()) * 43L;
        bits = bits + java.lang.Double.doubleToLongBits(getHeight()) * 47L;
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Rectangle2D) {
            Rectangle2D r2d = (Rectangle2D) obj;
            return ((getX() == r2d.getX())
                    && (getY() == r2d.getY())
                    && (getWidth() == r2d.getWidth())
                    && (getHeight() == r2d.getHeight()));
        }
        return false;
    }

    // --- internal factories (not API; see the header note) ---------------------------------------

    static Rectangle2D newDouble(double x, double y, double w, double h) {
        return new Double(x, y, w, h);
    }

    static Rectangle2D newFloat(float x, float y, float w, float h) {
        return new Float(x, y, w, h);
    }
}
