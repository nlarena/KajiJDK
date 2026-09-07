package java.awt.geom;

// KajiLibrary's java.awt.geom.Ellipse2D -- an ellipse inscribed in its frame. The surface is
// complete.
//
// Every geometric test is done by normalizing to a circle of radius 0.5 centred on the origin: one
// divides by the frame's width and height and subtracts 0.5. That way `contains` is a single
// `x²+y² < 0.25` instead of the ellipse's general equation, and the circular case needs no separate
// treatment. The strict `<` is the right one: a point exactly on the edge is **not** contained.
public abstract class Ellipse2D extends RectangularShape {

    // An ellipse with float coordinates.
    public static class Float extends Ellipse2D implements java.io.Serializable {

        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
        }

        public Float(float x, float y, float w, float h) {
            setFrame(x, y, w, h);
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
            return (this.width <= 0.0f || this.height <= 0.0f);
        }

        public void setFrame(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        public void setFrame(double x, double y, double w, double h) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
        }

        public Rectangle2D getBounds2D() {
            return Rectangle2D.newFloat(this.x, this.y, this.width, this.height);
        }
    }

    // An ellipse with double coordinates.
    public static class Double extends Ellipse2D implements java.io.Serializable {

        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double w, double h) {
            setFrame(x, y, w, h);
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
            return (this.width <= 0.0 || this.height <= 0.0);
        }

        public void setFrame(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        public Rectangle2D getBounds2D() {
            return Rectangle2D.newDouble(this.x, this.y, this.width, this.height);
        }
    }

    protected Ellipse2D() {
    }

    public boolean contains(double x, double y) {
        // Everything is carried over to a circle of radius 0.5 centred on the origin.
        double ellw = getWidth();
        if (ellw <= 0.0) {
            return false;
        }
        double normx = (x - getX()) / ellw - 0.5;
        double ellh = getHeight();
        if (ellh <= 0.0) {
            return false;
        }
        double normy = (y - getY()) / ellh - 0.5;
        return (normx * normx + normy * normy) < 0.25;
    }

    // The rectangle's point closest to the ellipse's centre is sought; if that point falls inside
    // the normalized unit circle, they intersect. Testing only the corners would be wrong: a
    // rectangle may cross the ellipse without any corner being inside.
    public boolean intersects(double x, double y, double w, double h) {
        if (w <= 0.0 || h <= 0.0) {
            return false;
        }
        double ellw = getWidth();
        if (ellw <= 0.0) {
            return false;
        }
        double normx0 = (x - getX()) / ellw - 0.5;
        double normx1 = normx0 + w / ellw;
        double ellh = getHeight();
        if (ellh <= 0.0) {
            return false;
        }
        double normy0 = (y - getY()) / ellh - 0.5;
        double normy1 = normy0 + h / ellh;
        double nearx;
        double neary;
        if (normx0 > 0.0) {
            // the centre is left of the rectangle
            nearx = normx0;
        } else if (normx1 < 0.0) {
            // the centre is right of it
            nearx = normx1;
        } else {
            nearx = 0.0;
        }
        if (normy0 > 0.0) {
            neary = normy0;
        } else if (normy1 < 0.0) {
            neary = normy1;
        } else {
            neary = 0.0;
        }
        return (nearx * nearx + neary * neary) < 0.25;
    }

    // The ellipse is convex, so the four corners are enough.
    public boolean contains(double x, double y, double w, double h) {
        return (contains(x, y)
                && contains(x + w, y)
                && contains(x, y + h)
                && contains(x + w, y + h));
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new EllipseIterator(this, at);
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
        if (obj instanceof Ellipse2D) {
            Ellipse2D e2d = (Ellipse2D) obj;
            return ((getX() == e2d.getX())
                    && (getY() == e2d.getY())
                    && (getWidth() == e2d.getWidth())
                    && (getHeight() == e2d.getHeight()));
        }
        return false;
    }
}
