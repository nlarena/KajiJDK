package java.awt.geom;

// KajiLibrary's java.awt.geom.Dimension2D -- width and height in floating point, with no
// location. The surface is complete (5 members).
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
            // It cannot happen: the class declares Cloneable.
            throw new InternalError(e.toString());
        }
    }
}
