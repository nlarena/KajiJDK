package java.awt;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// KajiLibrary's java.awt.Rectangle -- a rectangle with integer coordinates.
//
// Like Shape and Dimension, this class lives in `java.awt` but was written for java.awt.geom: it is
// the return type of `Shape.getBounds()` and therefore of `RectangularShape.getBounds()`,
// `Line2D.getBounds()`, `Path2D.getBounds()` and `Area.getBounds()`. Without it those members
// cannot be declared and the Shape interface does not even compile.
//
// **The surface is complete.** This note headed a list of what was missing, and every item in the
// list said it was already there:
//
//   * The members with `java.awt.Point` -- `Rectangle(Point)`, `Rectangle(Point, Dimension)`,
//     `getLocation()`, `setLocation(Point)`, `add(Point)`, `contains(Point)` -- are declared. * The
//     four 1.0 names -- `inside`, `move`, `reshape`, `resize` -- are declared too. In the JDK they
//     are not aliases: they are the methods that do the work and the new names delegate to them.
//     That direction is replicated because a subclass that overrides `reshape` --which is what code
//     of the time did-- has to keep seeing the `setBounds` calls go through it. The JDK marks those
//     four `@Deprecated`; they are not marked here. * `getSize()`/`setSize(Dimension)` are
//     declared.
//
// On the arithmetic of `setRect(double,...)`: it inherits from Rectangle2D a signature in `double`
// and has to fit it into four `int`. The clipping is not "cast and done": the origin is rounded
// down and the size up, so that the integer rectangle **contains** the floating-point one instead
// of cropping it, and a width outside the int range saturates at MAX_VALUE instead of wrapping
// around. A rectangle whose origin is already out of range is marked empty (width -1) because no
// integer represents it; returning a saturated rectangle would claim it covers something it does
// not.
public class Rectangle extends Rectangle2D implements Shape, java.io.Serializable {

    public int x;
    public int y;
    public int width;
    public int height;

    public Rectangle() {
        this(0, 0, 0, 0);
    }

    public Rectangle(Rectangle r) {
        this(r.x, r.y, r.width, r.height);
    }

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle(int width, int height) {
        this(0, 0, width, height);
    }

    public Rectangle(Point p) {
        this(p.x, p.y, 0, 0);
    }

    public Rectangle(Point p, Dimension d) {
        this(p.x, p.y, d.width, d.height);
    }

    public Rectangle(Dimension d) {
        this(0, 0, d.width, d.height);
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

    public Rectangle getBounds() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    public Rectangle2D getBounds2D() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    public void setBounds(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    public void setBounds(int x, int y, int width, int height) {
        reshape(x, y, width, height);
    }

    public void reshape(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setRect(double x, double y, double width, double height) {
        int newx;
        int newy;
        int neww;
        int newh;

        if (x > 2.0 * Integer.MAX_VALUE) {
            // So far in +X that no int represents it: it is marked empty instead of saturating.
            newx = Integer.MAX_VALUE;
            neww = -1;
        } else {
            newx = clip(x, false);
            if (width >= 0) {
                width = width + (x - newx);
            }
            neww = clipDim(width);
        }

        if (y > 2.0 * Integer.MAX_VALUE) {
            newy = Integer.MAX_VALUE;
            newh = -1;
        } else {
            newy = clip(y, false);
            if (height >= 0) {
                height = height + (y - newy);
            }
            newh = clipDim(height);
        }

        setBounds(newx, newy, neww, newh);
    }

    private static int clipDim(double v) {
        if (v < 0) {
            return -1;
        }
        if (v > 2.0 * Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return clip(v, true);
    }

    // doceil=false for origins (floor) and true for sizes (ceiling): the resulting integer
    // rectangle contains the floating-point one.
    private static int clip(double v, boolean doceil) {
        if (v <= (double) Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (v >= (double) Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (doceil) {
            return (int) Math.ceil(v);
        }
        return (int) Math.floor(v);
    }

    public void setSize(int width, int height) {
        resize(width, height);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }

    public Dimension getSize() {
        return new Dimension(this.width, this.height);
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

    public void move(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void translate(int dx, int dy) {
        int oldv = this.x;
        int newv = oldv + dx;
        if (dx < 0) {
            // Overflow downwards: the left edge saturates and the width is stretched so the right
            // edge does not move. If that does not fit either, the width saturates at MAX_VALUE
            // (this comment said the rectangle became empty).
            if (newv > oldv) {
                if (this.width >= 0) {
                    this.width = this.width + (newv - Integer.MIN_VALUE);
                    if (this.width < 0) {
                        this.width = Integer.MAX_VALUE;
                    }
                }
                newv = Integer.MIN_VALUE;
            }
        } else {
            if (newv < oldv) {
                if (this.width >= 0) {
                    this.width = this.width + (newv - Integer.MAX_VALUE);
                    if (this.width < 0) {
                        this.width = Integer.MAX_VALUE;
                    }
                }
                newv = Integer.MAX_VALUE;
            }
        }
        this.x = newv;

        oldv = this.y;
        newv = oldv + dy;
        if (dy < 0) {
            if (newv > oldv) {
                if (this.height >= 0) {
                    this.height = this.height + (newv - Integer.MIN_VALUE);
                    if (this.height < 0) {
                        this.height = Integer.MAX_VALUE;
                    }
                }
                newv = Integer.MIN_VALUE;
            }
        } else {
            if (newv < oldv) {
                if (this.height >= 0) {
                    this.height = this.height + (newv - Integer.MAX_VALUE);
                    if (this.height < 0) {
                        this.height = Integer.MAX_VALUE;
                    }
                }
                newv = Integer.MAX_VALUE;
            }
        }
        this.y = newv;
    }

    public void grow(int h, int v) {
        long x0 = (long) this.x - (long) h;
        long y0 = (long) this.y - (long) v;
        long w0 = (long) this.width + ((long) h * 2L);
        long h0 = (long) this.height + ((long) v * 2L);
        if (w0 < 0) {
            w0 = w0 - x0;
            if (w0 < Integer.MIN_VALUE) {
                w0 = Integer.MIN_VALUE;
            }
            if (x0 < Integer.MIN_VALUE) {
                x0 = Integer.MIN_VALUE;
            } else if (x0 > Integer.MAX_VALUE) {
                x0 = Integer.MAX_VALUE;
            }
        } else {
            if (x0 < Integer.MIN_VALUE) {
                w0 = w0 + x0 - Integer.MIN_VALUE;
                x0 = Integer.MIN_VALUE;
            } else if (x0 > Integer.MAX_VALUE) {
                w0 = 0;
                x0 = Integer.MAX_VALUE;
            }
            if (w0 > Integer.MAX_VALUE) {
                w0 = Integer.MAX_VALUE;
            }
        }
        if (h0 < 0) {
            h0 = h0 - y0;
            if (h0 < Integer.MIN_VALUE) {
                h0 = Integer.MIN_VALUE;
            }
            if (y0 < Integer.MIN_VALUE) {
                y0 = Integer.MIN_VALUE;
            } else if (y0 > Integer.MAX_VALUE) {
                y0 = Integer.MAX_VALUE;
            }
        } else {
            if (y0 < Integer.MIN_VALUE) {
                h0 = h0 + y0 - Integer.MIN_VALUE;
                y0 = Integer.MIN_VALUE;
            } else if (y0 > Integer.MAX_VALUE) {
                h0 = 0;
                y0 = Integer.MAX_VALUE;
            }
            if (h0 > Integer.MAX_VALUE) {
                h0 = Integer.MAX_VALUE;
            }
        }
        this.x = (int) x0;
        this.y = (int) y0;
        this.width = (int) w0;
        this.height = (int) h0;
    }

    public boolean isEmpty() {
        return (this.width <= 0) || (this.height <= 0);
    }

    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }

    public boolean contains(int x, int y) {
        return inside(x, y);
    }

    public boolean inside(int x, int y) {
        if (isEmpty()) {
            return false;
        }
        if (x < this.x || y < this.y) {
            return false;
        }
        long x2 = (long) this.x + (long) this.width;
        long y2 = (long) this.y + (long) this.height;
        return ((long) x < x2) && ((long) y < y2);
    }

    public boolean contains(int X, int Y, int W, int H) {
        if (isEmpty() || W <= 0 || H <= 0) {
            return false;
        }
        if (X < this.x || Y < this.y) {
            return false;
        }
        long x2 = (long) this.x + (long) this.width;
        long y2 = (long) this.y + (long) this.height;
        return (((long) X + (long) W) <= x2) && (((long) Y + (long) H) <= y2);
    }

    public boolean contains(Rectangle r) {
        return contains(r.x, r.y, r.width, r.height);
    }

    public boolean intersects(Rectangle r) {
        if (isEmpty() || r.width <= 0 || r.height <= 0) {
            return false;
        }
        long tx2 = (long) this.x + (long) this.width;
        long ty2 = (long) this.y + (long) this.height;
        long rx2 = (long) r.x + (long) r.width;
        long ry2 = (long) r.y + (long) r.height;
        return (rx2 > (long) this.x) && (ry2 > (long) this.y)
                && (tx2 > (long) r.x) && (ty2 > (long) r.y);
    }

    public Rectangle intersection(Rectangle r) {
        long tx1 = (long) this.x;
        long ty1 = (long) this.y;
        long rx1 = (long) r.x;
        long ry1 = (long) r.y;
        long tx2 = tx1 + (long) this.width;
        long ty2 = ty1 + (long) this.height;
        long rx2 = rx1 + (long) r.width;
        long ry2 = ry1 + (long) r.height;
        if (tx1 < rx1) {
            tx1 = rx1;
        }
        if (ty1 < ry1) {
            ty1 = ry1;
        }
        if (tx2 > rx2) {
            tx2 = rx2;
        }
        if (ty2 > ry2) {
            ty2 = ry2;
        }
        tx2 = tx2 - tx1;
        ty2 = ty2 - ty1;
        // tx2,ty2 can end up negative (no intersection); they are saturated to int without
        // normalizing, like Rectangle2D.intersect.
        if (tx2 < Integer.MIN_VALUE) {
            tx2 = Integer.MIN_VALUE;
        }
        if (ty2 < Integer.MIN_VALUE) {
            ty2 = Integer.MIN_VALUE;
        }
        return new Rectangle((int) tx1, (int) ty1, (int) tx2, (int) ty2);
    }

    public Rectangle union(Rectangle r) {
        long tx2 = (long) this.width;
        long ty2 = (long) this.height;
        if ((tx2 | ty2) < 0) {
            // This rectangle is "empty by negative size": the union is the other one as is.
            return new Rectangle(r);
        }
        long rx2 = (long) r.width;
        long ry2 = (long) r.height;
        if ((rx2 | ry2) < 0) {
            return new Rectangle(this);
        }
        long tx1 = (long) this.x;
        long ty1 = (long) this.y;
        tx2 = tx2 + tx1;
        ty2 = ty2 + ty1;
        long rx1 = (long) r.x;
        long ry1 = (long) r.y;
        rx2 = rx2 + rx1;
        ry2 = ry2 + ry1;
        if (tx1 > rx1) {
            tx1 = rx1;
        }
        if (ty1 > ry1) {
            ty1 = ry1;
        }
        if (tx2 < rx2) {
            tx2 = rx2;
        }
        if (ty2 < ry2) {
            ty2 = ry2;
        }
        tx2 = tx2 - tx1;
        ty2 = ty2 - ty1;
        if (tx2 > Integer.MAX_VALUE) {
            tx2 = Integer.MAX_VALUE;
        }
        if (ty2 > Integer.MAX_VALUE) {
            ty2 = Integer.MAX_VALUE;
        }
        return new Rectangle((int) tx1, (int) ty1, (int) tx2, (int) ty2);
    }

    public void add(Point pt) {
        add(pt.x, pt.y);
    }

    public void add(int newx, int newy) {
        if ((this.width | this.height) < 0) {
            this.x = newx;
            this.y = newy;
            this.width = 0;
            this.height = 0;
            return;
        }
        long x1 = (long) this.x;
        long y1 = (long) this.y;
        long x2 = x1 + (long) this.width;
        long y2 = y1 + (long) this.height;
        if (x1 > (long) newx) {
            x1 = (long) newx;
        }
        if (x2 < (long) newx) {
            x2 = (long) newx;
        }
        if (y1 > (long) newy) {
            y1 = (long) newy;
        }
        if (y2 < (long) newy) {
            y2 = (long) newy;
        }
        x2 = x2 - x1;
        y2 = y2 - y1;
        if (x2 > Integer.MAX_VALUE) {
            x2 = Integer.MAX_VALUE;
        }
        if (y2 > Integer.MAX_VALUE) {
            y2 = Integer.MAX_VALUE;
        }
        setBounds((int) x1, (int) y1, (int) x2, (int) y2);
    }

    public void add(Rectangle r) {
        Rectangle u = union(r);
        setBounds(u.x, u.y, u.width, u.height);
    }

    public int outcode(double x, double y) {
        int out = 0;
        if (this.width <= 0) {
            out = out | OUT_LEFT | OUT_RIGHT;
        } else if (x < (double) this.x) {
            out = out | OUT_LEFT;
        } else if (x > (double) this.x + (double) this.width) {
            out = out | OUT_RIGHT;
        }
        if (this.height <= 0) {
            out = out | OUT_TOP | OUT_BOTTOM;
        } else if (y < (double) this.y) {
            out = out | OUT_TOP;
        } else if (y > (double) this.y + (double) this.height) {
            out = out | OUT_BOTTOM;
        }
        return out;
    }

    public Rectangle2D createIntersection(Rectangle2D r) {
        if (r instanceof Rectangle) {
            return intersection((Rectangle) r);
        }
        Rectangle2D dest = new java.awt.geom.Rectangle2D.Double();
        Rectangle2D.intersect(this, r, dest);
        return dest;
    }

    public Rectangle2D createUnion(Rectangle2D r) {
        if (r instanceof Rectangle) {
            return union((Rectangle) r);
        }
        Rectangle2D dest = new java.awt.geom.Rectangle2D.Double();
        Rectangle2D.union(this, r, dest);
        return dest;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Rectangle) {
            Rectangle r = (Rectangle) obj;
            return ((this.x == r.x) && (this.y == r.y)
                    && (this.width == r.width) && (this.height == r.height));
        }
        return super.equals(obj);
    }

    public String toString() {
        return getClass().getName() + "[x=" + this.x + ",y=" + this.y
                + ",width=" + this.width + ",height=" + this.height + "]";
    }
}
