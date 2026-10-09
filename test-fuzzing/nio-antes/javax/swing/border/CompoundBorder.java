package javax.swing.border;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Two borders, one inside the other.
 *
 * <p>It is what justifies a border being an object and not a handful of properties of the
 * component: the result of combining two is another {@link Border}, indistinguishable from a
 * basic one, so they can be nested without limit -- a line inside a margin inside a title.
 *
 * <p>Either of the two may be {@code null}, and then this class behaves like the other one
 * alone. That allows building the combination without knowing beforehand whether both parts
 * exist.
 */
public class CompoundBorder extends AbstractBorder {

    private static final long serialVersionUID = 5231107617341800900L;

    protected Border outsideBorder;
    protected Border insideBorder;

    /** Both {@code null}: it neither draws nor takes up anything. */
    public CompoundBorder() {
        this.outsideBorder = null;
        this.insideBorder = null;
    }

    /** The outer one surrounding the inner one. */
    public CompoundBorder(Border outsideBorder, Border insideBorder) {
        this.outsideBorder = outsideBorder;
        this.insideBorder = insideBorder;
    }

    /**
     * Opaque only if <strong>both</strong> are.
     *
     * <p>An opaque one inside one that is not leaves the outer strip uncovered, so the promise
     * cannot be inherited from the stronger one.
     */
    public boolean isBorderOpaque() {
        boolean outside = this.outsideBorder == null || this.outsideBorder.isBorderOpaque();
        boolean inside = this.insideBorder == null || this.insideBorder.isBorderOpaque();
        return outside && inside;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Insets used = new Insets(0, 0, 0, 0);
        if (this.outsideBorder != null) {
            this.outsideBorder.paintBorder(c, g, x, y, width, height);
            used = this.outsideBorder.getBorderInsets(c);
        }
        // The inner one is painted in what the outer one left free. Hence the order: the outer one
        // first, because its size is what decides where the inner one starts.
        if (this.insideBorder != null) {
            this.insideBorder.paintBorder(c, g, x + used.left, y + used.top,
                    width - used.right - used.left, height - used.top - used.bottom);
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

    /** The outer border, or {@code null}. */
    public Border getOutsideBorder() {
        return this.outsideBorder;
    }

    /** The inner border, or {@code null}. */
    public Border getInsideBorder() {
        return this.insideBorder;
    }
}
