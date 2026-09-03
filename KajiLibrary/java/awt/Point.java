package java.awt;

import java.awt.geom.Point2D;

/**
 * Un punto con coordenadas enteras.
 *
 * <p>Hereda de {@code Point2D}, que ya estaba, y con eso se lleva gratis {@code distance()},
 * {@code hashCode()} y compania: lo unico propio es guardar dos {@code int} en vez de dos
 * {@code double}.
 *
 * <p>Lo que si es propio y facil de errar es {@code setLocation(double, double)}: hereda una firma
 * en coma flotante y tiene que meterla en enteros. El JDK **no** trunca, redondea al mas cercano
 * con {@code floor(v + 0.5)}, que no es lo mismo que {@code (int) v} para los negativos:
 * {@code setLocation(-2.6, -2.6)} da -3 y no -2. Truncar moveria el punto hacia el origen y
 * romperia la simetria de la funcion respecto del cero.
 *
 * <p>Que exista Point tambien completa {@code Rectangle}, que hasta ahora dejaba afuera todos sus
 * miembros con Point justamente porque el tipo no existia.
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
        // Contra un Point2D cualquiera se compara por valor, no por clase: un Point(3,4) y un
        // Point2D.Double(3,4) son el mismo punto y el JDK los da iguales en los dos sentidos.
        return super.equals(obj);
    }

    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + "]";
    }
}
