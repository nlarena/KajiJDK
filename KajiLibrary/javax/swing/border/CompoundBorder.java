package javax.swing.border;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Dos bordes, uno adentro del otro.
 *
 * <p>Es lo que justifica que un borde sea un objeto y no un puñado de propiedades del componente: el
 * resultado de combinar dos es otro {@link Border}, indistinguible de uno basico, asi que se pueden
 * anidar sin limite — una linea adentro de un margen adentro de un titulo.
 *
 * <p>Cualquiera de los dos puede ser {@code null}, y entonces esta clase se comporta como el otro
 * solo. Eso permite construir la combinacion sin saber de antemano si las dos partes existen.
 */
public class CompoundBorder extends AbstractBorder {

    private static final long serialVersionUID = 5231107617341800900L;

    protected Border outsideBorder;
    protected Border insideBorder;

    /** Los dos en {@code null}: no dibuja ni ocupa nada. */
    public CompoundBorder() {
        this.outsideBorder = null;
        this.insideBorder = null;
    }

    /** El de afuera rodeando al de adentro. */
    public CompoundBorder(Border outsideBorder, Border insideBorder) {
        this.outsideBorder = outsideBorder;
        this.insideBorder = insideBorder;
    }

    /**
     * Opaco solo si <strong>los dos</strong> lo son.
     *
     * <p>Uno opaco adentro de uno que no lo es deja sin cubrir la franja de afuera, asi que la
     * promesa no se puede heredar del mas fuerte.
     */
    public boolean isBorderOpaque() {
        boolean afuera = this.outsideBorder == null || this.outsideBorder.isBorderOpaque();
        boolean adentro = this.insideBorder == null || this.insideBorder.isBorderOpaque();
        return afuera && adentro;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Insets libre = new Insets(0, 0, 0, 0);
        if (this.outsideBorder != null) {
            this.outsideBorder.paintBorder(c, g, x, y, width, height);
            libre = this.outsideBorder.getBorderInsets(c);
        }
        // El de adentro se pinta en lo que dejo libre el de afuera. De ahi el orden: primero el
        // externo, porque su tamano es lo que decide donde empieza el interno.
        if (this.insideBorder != null) {
            this.insideBorder.paintBorder(c, g, x + libre.left, y + libre.top,
                    width - libre.right - libre.left, height - libre.top - libre.bottom);
        }
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.top = 0;
        insets.left = 0;
        insets.right = 0;
        insets.bottom = 0;
        if (this.outsideBorder != null) {
            Insets i = this.outsideBorder.getBorderInsets(c);
            insets.top = insets.top + i.top;
            insets.left = insets.left + i.left;
            insets.right = insets.right + i.right;
            insets.bottom = insets.bottom + i.bottom;
        }
        if (this.insideBorder != null) {
            Insets i = this.insideBorder.getBorderInsets(c);
            insets.top = insets.top + i.top;
            insets.left = insets.left + i.left;
            insets.right = insets.right + i.right;
            insets.bottom = insets.bottom + i.bottom;
        }
        return insets;
    }

    /** El borde de afuera, o {@code null}. */
    public Border getOutsideBorder() {
        return this.outsideBorder;
    }

    /** El borde de adentro, o {@code null}. */
    public Border getInsideBorder() {
        return this.insideBorder;
    }
}
