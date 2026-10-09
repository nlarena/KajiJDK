package java.awt;

import java.awt.geom.Point2D;

/**
 * A point with integer coordinates.
 *
 * <p>It inherits from {@code Point2D}, which already existed, and with that gets {@code
 * distance()}, {@code hashCode()} and company for free: the only thing of its own is keeping two
 * {@code int} instead of two {@code double}.
 *
 * <p>What is its own and easy to get wrong is {@code setLocation(double, double)}: it inherits a
 * floating-point signature and has to fit it into integers. The JDK does **not** truncate: it
 * rounds with {@code floor(v + 0.5)}, which is not the same as {@code (int) v} for negatives:
 * {@code setLocation(-2.6, -2.6)} gives -3 and not -2. Truncating would move the point towards the
 * origin. (This note added that truncating would break the function's symmetry around zero; {@code
 * floor(v + 0.5)} is not symmetric either: -2.5 gives -2 and 2.5 gives 3.)
 *
 * <p>Point's existence is also what let {@code Rectangle} declare its members that take a Point.
 */
public class Point extends Point2D implements java.io.Serializable {

    private static final long serialVersionUID = -5276940640259749850L;

    public int x;

    public int y;

    public Point() {
        this(0, 0);
    }

    public Point(Point p) {
        this(p.x, p.y);
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Point getLocation() {
        return new Point(x, y);
    }

    public void setLocation(Point p) {
        setLocation(p.x, p.y);
    }

    public void setLocation(int x, int y) {
        move(x, y);
    }

    public void setLocation(double x, double y) {
        this.x = (int) Math.floor(x + 0.5);
        this.y = (int) Math.floor(y + 0.5);
    }

    public void move(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void translate(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Point) {
            Point pt = (Point) obj;
            return (x == pt.x) && (y == pt.y);
        }
        // Against any Point2D it compares by value, not by class: a Point(3,4) and a
        // Point2D.Double(3,4) are the same point and the JDK gives them as equal both ways.
        return super.equals(obj);
    }

    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + "]";
    }
}
