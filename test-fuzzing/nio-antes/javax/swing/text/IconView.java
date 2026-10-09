package javax.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.Icon;

/**
 * An icon embedded in the text.
 *
 * <p>It is the simplest view there is: it cannot be split, it has no text inside and it takes up
 * what the icon takes up. It is aligned at the bottom ({@code getAlignment} returns 1 on the
 * vertical axis), which is what makes an icon in the middle of a line rest on the baseline
 * instead of floating.
 */
public class IconView extends View {

    private Icon c;

    /** A view of the icon that element has as an attribute. */
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

    /** It rests on the baseline; see the class note. */
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

    /** The left half is "before the icon", the right one "after". */
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
