package java.awt.geom;

// java.awt.geom.Dimension2D de KajiLibrary -- ancho y alto en coma flotante, sin ubicacion.
// Superficie completa (5 miembros).
public abstract class Dimension2D implements Cloneable {

    protected Dimension2D() {
    }

    public abstract double getWidth();

    public abstract double getHeight();

    public abstract void setSize(double width, double height);

    public void setSize(Dimension2D d) {
        setSize(d.getWidth(), d.getHeight());
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // No puede pasar: la clase declara Cloneable.
            throw new InternalError(e.toString());
        }
    }
}
