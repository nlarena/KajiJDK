package javax.swing.text;

import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.BoundedRangeModel;
import javax.swing.event.DocumentEvent;

/**
 * La vista de un campo de una sola linea.
 *
 * <h2>Dos cosas que un area de texto no hace</h2>
 *
 * <ul>
 * <li><strong>Se desplaza sola.</strong> Un campo no tiene barra: cuando el texto no entra, la
 * vista se corre para que el cursor quede visible. Eso es {@link #adjustAllocation}, que devuelve
 * un rectangulo mas ancho que el campo y corrido hacia la izquierda.
 * <li><strong>Se alinea.</strong> Si el texto entra de sobra, se ubica a la izquierda, al centro o
 * a la derecha segun lo que diga el componente, y en vertical siempre centrado. Un campo con el
 * texto pegado arriba se ve mal, y eso es lo que evita.
 * </ul>
 *
 * <p>Hereda de {@link PlainView} porque el dibujado de la linea es el mismo; lo unico que cambia es
 * donde cae esa linea.
 */
public class FieldView extends PlainView {

    public FieldView(Element elem) {
        super(elem);
    }

    /** Las metricas del componente. */
    protected FontMetrics getFontMetrics() {
        Container c = getContainer();
        return c.getFontMetrics(c.getFont());
    }

    /**
     * Corre y centra el rectangulo; ver la nota de la clase.
     *
     * <p>Devuelve un rectangulo del ancho del <em>texto</em>, no del campo, ubicado segun la
     * alineacion y el desplazamiento. Todo lo demas de la vista trabaja con ese rectangulo y no se
     * entera de nada.
     */
    protected Shape adjustAllocation(Shape a) {
        if (a != null) {
            Rectangle bounds = a.getBounds();
            int vspan = (int) getPreferredSpan(Y_AXIS);
            int hspan = (int) getPreferredSpan(X_AXIS);
            if (bounds.height != vspan) {
                int slop = bounds.height - vspan;
                bounds.y = bounds.y + slop / 2;
                bounds.height = bounds.height - slop;
            }

            Component c = getContainer();
            if (c instanceof JTextComponent) {
                JTextComponent tc = (JTextComponent) c;
                if (hspan < bounds.width) {
                    // Entra de sobra: se alinea.
                    bounds.width = hspan;
                } else {
                    // No entra: se corre para que se vea el cursor.
                    int x0 = bounds.x;
                    bounds.width = hspan;
                    Caret caret = tc.getCaret();
                    if (caret != null) {
                        try {
                            Shape s = super.modelToView(caret.getDot(), bounds,
                                    Position.Bias.Forward);
                            if (s != null) {
                                Rectangle cr = s.getBounds();
                                int visible = a.getBounds().width;
                                int dx = 0;
                                if (cr.x > x0 + visible) {
                                    dx = (x0 + visible) - cr.x - 1;
                                }
                                bounds.x = bounds.x + dx;
                            }
                        } catch (BadLocationException e) {
                            // Sin cursor ubicable, se deja donde estaba.
                        }
                    }
                }
            }
            return bounds;
        }
        return null;
    }

    /** Acomoda el modelo de desplazamiento del campo, si lo hay. */
    void updateVisibilityModel() {
    }

    public void paint(Graphics g, Shape a) {
        Rectangle r = (Rectangle) a;
        g.clipRect(r.x, r.y, r.width, r.height);
        super.paint(g, a);
    }

    Shape adjustPaintRegion(Shape a) {
        return adjustAllocation(a);
    }

    /** El ancho es el del texto; el alto, el de una linea. */
    public float getPreferredSpan(int axis) {
        if (axis == View.X_AXIS) {
            Segment buff = getLineBuffer();
            Document doc = getDocument();
            int p0 = getStartOffset();
            int p1 = getEndOffset();
            try {
                doc.getText(p0, p1 - p0, buff);
            } catch (BadLocationException e) {
                return 0;
            }
            FontMetrics fm = getFontMetrics();
            int width = (int) Utilities.getTabbedTextWidth(buff, fm, 0, this, p0);
            return width;
        }
        if (axis == View.Y_AXIS) {
            return getFontMetrics().getHeight();
        }
        throw new IllegalArgumentException("Invalid axis: " + axis);
    }

    /** Se estira a lo ancho y no a lo alto: un campo tiene una sola linea. */
    public int getResizeWeight(int axis) {
        if (axis == View.X_AXIS) {
            return 1;
        }
        return 0;
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        return super.modelToView(pos, adjustAllocation(a), b);
    }

    public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {
        return super.viewToModel(fx, fy, adjustAllocation(a), bias);
    }

    public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        super.insertUpdate(changes, adjustAllocation(a), f);
        updateVisibilityModel();
    }

    public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        super.removeUpdate(changes, adjustAllocation(a), f);
        updateVisibilityModel();
    }
}
