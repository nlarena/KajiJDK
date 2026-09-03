package java.awt;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// java.awt.Rectangle de KajiLibrary -- rectangulo con coordenadas enteras.
//
// Igual que Shape y Dimension, esta clase vive en `java.awt` pero se escribio por java.awt.geom: es
// el tipo de retorno de `Shape.getBounds()` y por lo tanto de `RectangularShape.getBounds()`,
// `Line2D.getBounds()`, `Path2D.getBounds()` y `Area.getBounds()`. Sin ella esos miembros no se
// pueden declarar y la interfaz Shape ni siquiera compila.
//
// **Superficie deliberadamente parcial.** `java.awt` no es esta tarea y no esta medido. Lo que falta
// y por que:
//
//   * Los miembros con `java.awt.Point` -- `Rectangle(Point)`, `Rectangle(Point, Dimension)`,
//     `getLocation()`, `setLocation(Point)`, `add(Point)`, `contains(Point)` -- **ya estan**:
//     faltaban solo porque `java.awt.Point` no existia, y ahora existe.
//   * Los cuatro nombres de 1.0 -- `inside`, `move`, `reshape`, `resize` -- tambien estan. Estan
//     obsoletos desde 1.1 pero son API publica, y en el JDK no son alias: son los metodos que
//     hacen el trabajo y los nombres nuevos delegan en ellos. Se replica esa direccion porque una
//     subclase que redefina `reshape` --que es lo que hacia el codigo de la epoca-- tiene que
//     seguir viendo pasar por ahi las llamadas a `setBounds`.
//   * `getSize()`/`setSize(Dimension)` si estan: Dimension ya existe (la escribio la geometria por
//     `RectangularShape.setFrame(Point2D, Dimension2D)`).
//
// Sobre la aritmetica de `setRect(double,...)`: hereda de Rectangle2D una firma en `double` y tiene
// que meterla en cuatro `int`. El recorte no es "castear y listo": el origen se redondea hacia
// abajo y la dimension hacia arriba, para que el rectangulo entero **contenga** al de coma flotante
// en vez de recortarlo, y un ancho que se sale del rango de int se satura en MAX_VALUE en vez de
// dar la vuelta. Un rectangulo cuyo origen ya esta fuera de rango se marca vacio (ancho -1) porque
// no hay ningun entero que lo represente; devolver un rectangulo saturado seria decir que cubre
// algo que no cubre.
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
            // Tan lejos en +X que ningun int lo representa: se marca vacio en vez de saturar.
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

    // doceil=false para origenes (piso) y true para dimensiones (techo): el entero resultante
    // contiene al rectangulo de coma flotante.
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
            // Desbordamiento hacia abajo: el borde izquierdo se satura y el ancho se estira para
            // que el borde derecho no se mueva. Si tampoco entra, el rectangulo queda vacio.
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
        // tx2,ty2 pueden quedar negativos (no hay interseccion); se saturan a int sin normalizar,
        // igual que Rectangle2D.intersect.
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
            // Este rectangulo es "vacio por dimension negativa": la union es el otro tal cual.
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
