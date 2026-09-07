package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * El separador de adentro de un menu.
 *
 * <p>Es el mismo surco de dos lineas que {@link MetalSeparatorUI}, pero pide cuatro pixeles de
 * alto en vez de dos y dibuja las lineas en el medio. Los dos pixeles de aire de cada lado son lo
 * que separa el surco de los items de arriba y de abajo; sin ellos un menu queda apretado y el
 * separador se confunde con el borde de un item.
 *
 * <p>La orientacion no se mira: un separador de menu es siempre horizontal.
 */
public class MetalPopupMenuSeparatorUI extends MetalSeparatorUI {

    public MetalPopupMenuSeparatorUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalPopupMenuSeparatorUI();
    }

    public void paint(Graphics g, JComponent c) {
        Dimension s = c.getSize();
        g.setColor(c.getForeground());
        g.drawLine(0, 1, s.width, 1);
        g.setColor(c.getBackground());
        g.drawLine(0, 2, s.width, 2);
    }

    /** Cuatro: dos de linea y dos de aire; ver la nota de la clase. */
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(0, 4);
    }
}
