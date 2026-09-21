package javax.swing;

import java.awt.Component;
import java.awt.Graphics;

/**
 * Something that knows how to draw itself at a fixed size.
 *
 * <h2>Why it is not simply an image</h2>
 *
 * <p>A {@link java.awt.Image} is pixels already decided. An icon is <strong>a drawing
 * instruction</strong>: it is asked to paint itself somewhere, and it may resolve that however
 * it likes -- with an image, with strokes, or by looking at the component that asks in order to
 * choose a colour that suits the theme --. Hence {@link #paintIcon} receives the
 * {@link Component}: it is not decoration, it is what allows one and the same icon to look
 * different on an enabled button and on a disabled one.
 *
 * <p>The size is declared separately and in advance, because whoever does the layout needs to
 * know how much room it takes <em>before</em> anything is drawn.
 */
public interface Icon {

    /**
     * It is drawn with its top left corner at {@code (x, y)}.
     *
     * @param c the component that asks for it, which the icon may consult; it may be {@code null}
     */
    void paintIcon(Component c, Graphics g, int x, int y);

    /** How much it measures in width. */
    int getIconWidth();

    /** How much it measures in height. */
    int getIconHeight();
}
