package javax.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.Icon;

/**
 * Un icono incrustado en el texto.
 *
 * <p>Es la vista mas simple que hay: no se puede partir, no tiene texto adentro y ocupa lo que
 * ocupa el icono. Se alinea por abajo ({@code getAlignment} devuelve 1 en el eje vertical), que es
 * lo que hace que un icono en medio de una linea se apoye sobre la linea de base en vez de flotar.
 */
public class IconView extends View {

    private Icon c;

    /** Una vista del icono que ese elemento tiene como atributo. */
    public IconView(Element elem) {
        super(elem);
        AttributeSet attr = elem.getAttributes();
        c = StyleConstants.getIcon(attr);
    }

    public void paint(Graphics g, Shape a) {
        Rectangle alloc = a.getBounds();
        c.paintIcon(getContainer(), g, alloc.x, alloc.y);
    }

    public float getPreferredSpan(int axis) {
        if (axis == View.X_AXIS) {
            return c.getIconWidth();
        }
        if (axis == View.Y_AXIS) {
            return c.getIconHeight();
        }
        throw new IllegalArgumentException("Invalid axis: " + axis);
    }

    /** Se apoya sobre la linea de base; ver la nota de la clase. */
    public float getAlignment(int axis) {
        if (axis == View.Y_AXIS) {
            return 1;
        }
        return super.getAlignment(axis);
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        if ((pos >= p0) && (pos <= p1)) {
            Rectangle r = a.getBounds();
            if (pos == p1) {
                r.x = r.x + r.width;
            }
            r.width = 0;
            return r;
        }
        throw new BadLocationException(pos + " not in range " + p0 + "," + p1, pos);
    }

    /** La mitad izquierda es "antes del icono", la derecha "despues". */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle alloc = (Rectangle) a;
        if (x < alloc.x + (alloc.width / 2)) {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }
        bias[0] = Position.Bias.Backward;
        return getEndOffset();
    }
}
