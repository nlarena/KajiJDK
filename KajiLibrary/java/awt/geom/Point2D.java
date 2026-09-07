package java.awt.geom;

// KajiLibrary's java.awt.geom.Point2D -- an (x,y) point in floating point. The surface is
// complete.
//
// Mind this while reading the file: inside Point2D's body, `Double` and `Float` are the nested
// classes from here, not java.lang's. That is why every use of the wrapper is written out in full
// (`java.lang.Double.doubleToLongBits`). It is not fussiness: it is §6.5.5's shadowing rule and the
// compiler applies it.
//
// And that is why the `newDouble`/`newFloat`/`newLike` factories at the end exist, which are
// internal and not API. The rest of the package uses them instead of writing
// `new Point2D.Double(...)` because this house's javac **cannot hold both `Double` names alive in
// the same compilation unit**: it keeps a single simple-name -> type map per file, so a file naming
// both `Point2D.Double` and `java.lang.Double` resolves one of the two wrongly (it gives
// "tipo incompatible" or "no se encuentra el campo MIN_VALUE"). That clash affected
// AffineTransform, Arc2D, Line2D and Path2D, which need both things. In here there is no clash
// --bare `Double` is already the nested one-- so construction lives here and the other files never
// name `Point2D.Double`. It is a detour around a compiler bug, not around the language
// specification: against the JDK's javac both forms are equally valid.
public abstract class Point2D implements Cloneable {

    // A point with float coordinates. It keeps them in float but exposes them in double, which is
    // what the base class's contract calls for.
    public static class Float extends Point2D implements java.io.Serializable {

        public float x;
        public float y;

        public Float() {
        }

        public Float(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return (double) this.x;
        }

        public double getY() {
            return (double) this.y;
        }

        public void setLocation(double x, double y) {
            this.x = (float) x;
            this.y = (float) y;
        }

        public void setLocation(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public String toString() {
            return "Point2D.Float[" + this.x + ", " + this.y + "]";
        }
    }

    // A point with double coordinates.
    public static class Double extends Point2D implements java.io.Serializable {

        public double x;
        public double y;

        public Double() {
        }

        public Double(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public void setLocation(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public String toString() {
            return "Point2D.Double[" + this.x + ", " + this.y + "]";
        }
    }

    protected Point2D() {
    }

    public abstract double getX();

    public abstract double getY();

    public abstract void setLocation(double x, double y);

    public void setLocation(Point2D p) {
        setLocation(p.getX(), p.getY());
    }

    // The squared distance exists apart from the distance on purpose: it avoids the root when
    // distances are only going to be compared with each other, and there the result is exact if the
    // operands are.
    public static double distanceSq(double x1, double y1, double x2, double y2) {
        x1 = x1 - x2;
        y1 = y1 - y2;
        return (x1 * x1 + y1 * y1);
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        x1 = x1 - x2;
        y1 = y1 - y2;
        return Math.sqrt(x1 * x1 + y1 * y1);
    }

    public double distanceSq(double px, double py) {
        px = px - getX();
        py = py - getY();
        return (px * px + py * py);
    }

    public double distanceSq(Point2D pt) {
        double px = pt.getX() - getX();
        double py = pt.getY() - getY();
        return (px * px + py * py);
    }

    public double distance(double px, double py) {
        px = px - getX();
        py = py - getY();
        return Math.sqrt(px * px + py * py);
    }

    public double distance(Point2D pt) {
        double px = pt.getX() - getX();
        double py = pt.getY() - getY();
        return Math.sqrt(px * px + py * py);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }

    // A mix of the two coordinates' bits. The JDK's formula is replicated and not another one:
    // hashCode is observable and two implementations that "hash well" but differently are
    // distinguishable.
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits = bits ^ (java.lang.Double.doubleToLongBits(getY()) * 31L);
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    public boolean equals(Object obj) {
        if (obj instanceof Point2D) {
            Point2D p2d = (Point2D) obj;
            return (getX() == p2d.getX()) && (getY() == p2d.getY());
        }
        return super.equals(obj);
    }

    // --- internal factories (not API; see the header note) ---------------------------------------

    static Point2D newDouble(double x, double y) {
        return new Double(x, y);
    }

    static Point2D newFloat(float x, float y) {
        return new Float(x, y);
    }

    // An empty point of the same precision as `src`. It is the JDK's rule for the implicit
    // destination of AffineTransform.transform(src, null): Double only if the source already was a
    // Double, Float in every other case --a subclass of Point2D written outside included.
    static Point2D newLike(Point2D src) {
        if (src instanceof Double) {
            return new Double();
        }
        return new Float();
    }
}
