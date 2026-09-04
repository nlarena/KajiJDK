package javax.swing.plaf;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;

/**
 * Un {@link Icon} que puso el aspecto; ver {@link UIResource}.
 *
 * <p>A diferencia de color, fuente o insets, un icono no se puede heredar —es una interfaz—, asi
 * que este envuelve al icono real y le delega todo. Es la unica manera de ponerle la etiqueta a un
 * icono que ya existe.
 */
public class IconUIResource implements Icon, UIResource, Serializable {

    private Icon delegate;

    /** Envuelve ese icono. {@code null} no es un icono. */
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
