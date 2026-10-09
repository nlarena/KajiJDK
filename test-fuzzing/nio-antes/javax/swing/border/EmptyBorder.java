package javax.swing.border;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * A border that only reserves space and draws nothing.
 *
 * <p>It sounds like nothing and it is one of the most used: it is how a margin is put on a
 * component in Swing. There is no "margin" property -- there is a border that takes up room and
 * does not paint.
 *
 * <p>It is not opaque, and that is right precisely because it does not draw: if it said yes,
 * Swing would skip the background underneath and pixels would be left unpainted.
 */
public class EmptyBorder extends AbstractBorder implements java.io.Serializable {

    private static final long serialVersionUID = -8116076291731988694L;

    protected int left;
    protected int right;
    protected int top;
    protected int bottom;

    /** With the four margins in pixels. */
    public EmptyBorder(int top, int left, int bottom, int right) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    /** With the four margins an {@link Insets} carries. */
    public EmptyBorder(Insets borderInsets) {
        this.top = borderInsets.top;
        this.right = borderInsets.right;
        this.bottom = borderInsets.bottom;
        this.left = borderInsets.left;
    }

    /** It draws nothing, which is the whole point of this class. */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = this.left;
        insets.top = this.top;
        insets.right = this.right;
        insets.bottom = this.bottom;
        return insets;
    }

    /** The margins, in a new {@link Insets}. */
    public Insets getBorderInsets() {
        return new Insets(this.top, this.left, this.bottom, this.right);
    }

    public boolean isBorderOpaque() {
        return false;
    }
}
