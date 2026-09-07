package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * El separador de un menu desplegable.
 *
 * <p>Igual que {@link BasicSeparatorUI} salvo en una cosa: dentro de un menu no hay mas que una
 * orientacion posible, asi que no la mira, y las dos lineas se dibujan <em>debajo</em> del margen
 * de arriba en vez de pegadas al borde del componente. Por eso redefine {@link #paint} y
 * {@link #getPreferredSize}.
 */
public class BasicPopupMenuSeparatorUI extends BasicSeparatorUI {

    public BasicPopupMenuSeparatorUI() {
    }

    /** Uno nuevo cada vez, como el de la clase de la que sale. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicPopupMenuSeparatorUI();
    }

    /** Las dos lineas, arrancando despues del margen de arriba. */
    public void paint(Graphics g, JComponent c) {
        Dimension s = c.getSize();
        Insets insets = c.getInsets();
        int y = insets.top;
        g.setColor(c.getForeground());
        g.drawLine(0, y, s.width, y);
        g.setColor(c.getBackground());
        g.drawLine(0, y + 1, s.width, y + 1);
    }

    /** Alto de dos mas los margenes; el ancho lo pone el menu. */
    public Dimension getPreferredSize(JComponent c) {
        Insets insets = c.getInsets();
        return new Dimension(0, insets.top + insets.bottom + 2);
    }
}
