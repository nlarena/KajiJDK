package java.awt;

import java.awt.geom.Dimension2D;

// KajiLibrary's java.awt.Dimension -- integer width and height.
//
// It was written first because `RectangularShape.setFrame(Point2D, Dimension2D)` and
// `Arc2D.setArc(Point2D, Dimension2D, ...)` ask for a Dimension2D, and Dimension is its only
// concrete implementation in the JDK. This note called its surface deliberately limited to what the
// geometry uses; it declares every member the JDK's has.
public class Dimension extends Dimension2D implements java.io.Serializable {

    public int width;
    public int height;

    public Dimension() {
        this(0, 0);
    }

    public Dimension(Dimension d) {
        this(d.width, d.height);
    }

    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return (double) this.width;
    }

    public double getHeight() {
        return (double) this.height;
    }

    // The JDK rounds with Math.ceil and saturates at Integer.MAX_VALUE; it is replicated as is
    // because it is observable from `setFrame(Point2D, Dimension2D)`.
    public void setSize(double width, double height) {
        this.width = clamp(Math.ceil(width));
        this.height = clamp(Math.ceil(height));
    }

    private static int clamp(double v) {
        if (v < (double) java.lang.Integer.MIN_VALUE) {
            return java.lang.Integer.MIN_VALUE;
        }
        if (v > (double) java.lang.Integer.MAX_VALUE) {
            return java.lang.Integer.MAX_VALUE;
        }
        return (int) v;
    }

    public Dimension getSize() {
        return new Dimension(this.width, this.height);
    }

    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Dimension) {
            Dimension d = (Dimension) obj;
            return (this.width == d.width) && (this.height == d.height);
        }
        return false;
    }

    public int hashCode() {
        int sum = this.width + this.height;
        return sum * (sum + 1) / 2 + this.width;
    }

    /**
     * The name of the real class and the two sizes.
     *
     * <p>The real class, not a fixed {@code java.awt.Dimension}: a {@code DimensionUIResource} has
     * to say it is one. It is what makes visible where a size came from --the look and feel or the
     * program-- when it is printed.
     */
    public String toString() {
        return getClass().getName() + "[width=" + this.width + ",height=" + this.height + "]";
    }
}
