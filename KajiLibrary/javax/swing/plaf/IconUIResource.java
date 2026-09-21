package javax.swing.plaf;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;

/**
 * An {@link Icon} the look and feel set; see {@link UIResource}.
 *
 * <p>Unlike a colour, a font or insets, an icon cannot be inherited from --it is an interface--,
 * so this one wraps the real icon and delegates everything to it. It is the only way of putting
 * the label on an icon that already exists.
 */
public class IconUIResource implements Icon, UIResource, Serializable {

    private Icon delegate;

    /** Wraps that icon. {@code null} is not an icon. */
    public IconUIResource(Icon delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("null delegate icon argument");
        }
        this.delegate = delegate;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        delegate.paintIcon(c, g, x, y);
    }

    public int getIconWidth() {
        return delegate.getIconWidth();
    }

    public int getIconHeight() {
        return delegate.getIconHeight();
    }
}
