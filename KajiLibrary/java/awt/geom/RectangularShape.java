package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;

// java.awt.geom.RectangularShape de KajiLibrary -- la base de toda figura que queda definida por su
// "marco" rectangular: Rectangle2D, Ellipse2D, RoundRectangle2D y Arc2D. Superficie completa.
//
// La clase no sabe nada de la figura en si; solo administra el marco (x, y, ancho, alto) y deriva de
// ahi los accesos de conveniencia. Las preguntas geometricas de verdad (`contains`, `intersects`,
// `getPathIterator`) las contesta cada subclase.
public abstract class RectangularShape implements Shape, Cloneable {

    protected RectangularShape() {
    }

    public abstract double getX();

    public abstract double getY();

    public abstract double getWidth();

    public abstract double getHeight();

    public double getMinX() {
        return getX();
    }

    public double getMinY() {
        return getY();
    }

    public double getMaxX() {
        return getX() + getWidth();
    }

    public double getMaxY() {
        return getY() + getHeight();
    }

    public double getCenterX() {
        return getX() + getWidth() / 2.0;
    }

    public double getCenterY() {
        return getY() + getHeight() / 2.0;
    }

    public Rectangle2D getFrame() {
        return Rectangle2D.newDouble(getX(), getY(), getWidth(), getHeight());
    }

    public abstract boolean isEmpty();

    public abstract void setFrame(double x, double y, double w, double h);

    public void setFrame(Point2D loc, Dimension2D size) {
        setFrame(loc.getX(), loc.getY(), size.getWidth(), size.getHeight());
    }

    public void setFrame(Rectangle2D r) {
        setFrame(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    // Los dos puntos son esquinas opuestas, en cualquier orden: se normaliza para que el marco
    // resultante nunca tenga ancho o alto negativos.
    public void setFrameFromDiagonal(double x1, double y1, double x2, double y2) {
        if (x2 < x1) {
            double t = x1;
            x1 = x2;
            x2 = t;
        }
        if (y2 < y1) {
            double t = y1;
            y1 = y2;
            y2 = t;
        }
        setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    public void setFrameFromDiagonal(Point2D p1, Point2D p2) {
        setFrameFromDiagonal(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public void setFrameFromCenter(double centerX, double centerY,
                                   double cornerX, double cornerY) {
        double halfW = Math.abs(cornerX - centerX);
        double halfH = Math.abs(cornerY - centerY);
        setFrame(centerX - halfW, centerY - halfH, halfW * 2.0, halfH * 2.0);
    }

    public void setFrameFromCenter(Point2D center, Point2D corner) {
        setFrameFromCenter(center.getX(), center.getY(), corner.getX(), corner.getY());
    }

    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    // El rectangulo entero mas chico que contiene a la figura: piso abajo/izquierda y techo
    // arriba/derecha. Un marco de ancho o alto negativo no representa nada y devuelve el vacio.
    public Rectangle getBounds() {
        double width = getWidth();
        double height = getHeight();
        if (width < 0 || height < 0) {
            return new Rectangle();
        }
        double x = getX();
        double y = getY();
        double x1 = Math.floor(x);
        double y1 = Math.floor(y);
        double x2 = Math.ceil(x + width);
        double y2 = Math.ceil(y + height);
        return new Rectangle((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(getPathIterator(at), flatness);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
}
