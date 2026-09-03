package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// java.awt.Shape de KajiLibrary.
//
// Vive en `java.awt` y no en `java.awt.geom`, pero todo el paquete geom lo necesita: es el tipo que
// devuelven `AffineTransform.createTransformedShape` y `Path2D.createTransformedShape`, y el que
// toman `new Area(Shape)` y `new GeneralPath(Shape)`. Se escribio aca por esa dependencia, no como
// principio de implementar java.awt: el resto del paquete padre (Component, Graphics, Toolkit...)
// sigue sin empezar y no hace falta para la geometria.
//
// Nota sobre `contains`: la definicion de la spec es "insideness" -- un punto sobre el borde
// izquierdo o superior pertenece a la figura, uno sobre el derecho o inferior no. Esa asimetria es
// deliberada (hace que figuras adyacentes teselen sin solaparse) y esta respetada en todo el paquete.
public interface Shape {

    public abstract Rectangle getBounds();

    public abstract Rectangle2D getBounds2D();

    public abstract boolean contains(double x, double y);

    public abstract boolean contains(Point2D p);

    public abstract boolean intersects(double x, double y, double w, double h);

    public abstract boolean intersects(Rectangle2D r);

    public abstract boolean contains(double x, double y, double w, double h);

    public abstract boolean contains(Rectangle2D r);

    public abstract PathIterator getPathIterator(AffineTransform at);

    public abstract PathIterator getPathIterator(AffineTransform at, double flatness);
}
