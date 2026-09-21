package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * A {@link BevelBorder} with softened corners.
 *
 * <p>The only difference is that the corners do not close: the lines stop one pixel short, and
 * the relief is left with rounded tips instead of right angles. The effect is subtle and the
 * reason is one of style, not function.
 *
 * <p>It has a real consequence all the same: <strong>it stops being opaque</strong>. The corner
 * pixels are left unpainted, and promising the opposite would leave rubbish right there -- the
 * same argument as in {@link LineBorder} with rounded corners.
 */
public class SoftBevelBorder extends BevelBorder {

    private static final long serialVersionUID = 5248789787305979975L;

    /** With the colours derived from the component's background. */
    public SoftBevelBorder(int bevelType) {
        super(bevelType);
    }

    /** With one light colour and one dark one. */
    public SoftBevelBorder(int bevelType, Color highlight, Color shadow) {
        super(bevelType, highlight, shadow);
    }

    /** With the four shades explicit. */
    public SoftBevelBorder(int bevelType, Color highlightOuterColor, Color highlightInnerColor,
            Color shadowOuterColor, Color shadowInnerColor) {
        super(bevelType, highlightOuterColor, highlightInnerColor, shadowOuterColor,
                shadowInnerColor);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color old = g.getColor();
        g.translate(x, y);

        boolean raised = getBevelType() == RAISED;
        Color outerHighlight = raised ? getHighlightOuterColor(c) : getShadowInnerColor(c);
        Color innerHighlight = raised ? getHighlightInnerColor(c) : getShadowOuterColor(c);
        Color outerShadow = raised ? getShadowOuterColor(c) : getHighlightInnerColor(c);
        Color innerShadow = raised ? getShadowInnerColor(c) : getHighlightOuterColor(c);

        // The lines start and end one pixel further in than in `BevelBorder`: that is the whole
        // softening, and it is also why the corners are left unpainted.
        g.setColor(outerHighlight);
        g.drawLine(0, 0, width - 2, 0);
        g.drawLine(0, 1, 0, height - 2);

        g.setColor(innerHighlight);
        g.drawLine(1, 1, width - 3, 1);
        g.drawLine(1, 2, 1, height - 3);

        g.setColor(outerShadow);
        g.drawLine(1, height - 1, width - 1, height - 1);
        g.drawLine(width - 1, 1, width - 1, height - 2);

        g.setColor(innerShadow);
        g.drawLine(2, height - 2, width - 2, height - 2);
        g.drawLine(width - 2, 2, width - 2, height - 3);

        g.translate(-x, -y);
        g.setColor(old);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 3;
        insets.top = 3;
        insets.right = 3;
        insets.bottom = 3;
        return insets;
    }

    /** It is not opaque: the corners are left unpainted. See the class note. */
    public boolean isBorderOpaque() {
        return false;
    }
}
