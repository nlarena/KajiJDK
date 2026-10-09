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
 * The view of a single-line field.
 *
 * <h2>Two things a text area does not do</h2>
 *
 * <ul>
 * <li><strong>It scrolls itself.</strong> A field has no bar: when the text does not fit, the
 * view shifts so that the cursor stays visible. That is {@link #adjustAllocation}, which returns
 * a rectangle wider than the field and shifted to the left.
 * <li><strong>It aligns itself.</strong> If the text fits with room to spare, it is placed to
 * the left, in the centre or to the right according to what the component says, and vertically
 * always centred. A field with the text stuck to the top looks wrong, and that is what this
 * avoids.
 * </ul>
 *
 * <p>It inherits from {@link PlainView} because the drawing of the line is the same; the only
 * thing that changes is where that line falls.
 */
public class FieldView extends PlainView {

    public FieldView(Element elem) {
        super(elem);
    }

    /** The component's metrics. */
    protected FontMetrics getFontMetrics() {
        Container c = getContainer();
        return c.getFontMetrics(c.getFont());
    }

    /**
     * It shifts and centres the rectangle; see the class note.
     *
     * <p>It returns a rectangle of the <em>text</em>'s width, not the field's, placed according to
     * the alignment and the scrolling. Everything else in the view works with that rectangle and
     * hears about nothing.
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
                    // It fits with room to spare: it is aligned.
                    bounds.width = hspan;
                } else {
                    // It does not fit: it shifts so that the cursor is seen.
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
                            // With no placeable cursor, it is left where it was.
                        }
                    }
                }
            }
            return bounds;
        }
        return null;
    }

    /** It fixes up the field's scrolling model, if there is one. */
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

    /** The width is the text's; the height, that of one line. */
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

    /** It stretches widthwise and not heightwise: a field has a single line. */
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
