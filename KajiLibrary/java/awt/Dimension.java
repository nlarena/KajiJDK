package java.awt;

import java.awt.geom.Dimension2D;

// java.awt.Dimension de KajiLibrary -- ancho y alto enteros.
//
// Esta aca porque `RectangularShape.setFrame(Point2D, Dimension2D)` y `Arc2D.setArc(Point2D,
// Dimension2D, ...)` piden un Dimension2D, y Dimension es su unica implementacion concreta en el
// JDK. Superficie deliberadamente acotada a lo que la geometria usa; `java.awt` no es esta tarea.
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

    // El JDK redondea con Math.ceil y satura en Integer.MAX_VALUE; se replica tal cual porque es
    // observable desde `setFrame(Point2D, Dimension2D)`.
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

    public String toString() {
        return "java.awt.Dimension[width=" + this.width + ",height=" + this.height + "]";
    }
}
