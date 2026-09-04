package javax.swing.border;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Un borde que solo reserva espacio y no dibuja nada.
 *
 * <p>Suena a nada y es de los mas usados: es como se le pone margen a un componente en Swing. No hay
 * una propiedad "margen" — hay un borde que ocupa lugar y no pinta.
 *
 * <p>No es opaco, y eso es lo correcto justamente porque no dibuja: si dijera que si, Swing se
 * saltearia el fondo de abajo y quedarian pixeles sin pintar.
 */
public class EmptyBorder extends AbstractBorder implements java.io.Serializable {

    private static final long serialVersionUID = -8116076291731988694L;

    protected int left;
    protected int right;
    protected int top;
    protected int bottom;

    /** Con los cuatro margenes en pixeles. */
    public EmptyBorder(int top, int left, int bottom, int right) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    /** Con los cuatro margenes que trae un {@link Insets}. */
    public EmptyBorder(Insets borderInsets) {
        this.top = borderInsets.top;
        this.right = borderInsets.right;
        this.bottom = borderInsets.bottom;
        this.left = borderInsets.left;
    }

    /** No dibuja nada, que es todo el punto de esta clase. */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = this.left;
        insets.top = this.top;
        insets.right = this.right;
        insets.bottom = this.bottom;
        return insets;
    }

    /** Los margenes, en un {@link Insets} nuevo. */
    public Insets getBorderInsets() {
        return new Insets(this.top, this.left, this.bottom, this.right);
    }

    public boolean isBorderOpaque() {
        return false;
    }
}
