package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * El divisor de un panel dividido de Metal.
 *
 * <p>No es publica y no esta en el censo, pero se ve: {@code createDefaultDivider().getClass()}
 * la devuelve, y un programa puede preguntarle el nombre.
 *
 * <p>Lo unico que dibuja de mas que el basico son los tres puntos del medio, que es lo que le dice
 * al que mira que esa franja gris se puede arrastrar. Sin ellos un divisor de Metal es
 * indistinguible de un separador.
 */
class MetalSplitPaneDivider extends BasicSplitPaneDivider {

    MetalSplitPaneDivider(BasicSplitPaneUI ui) {
        super(ui);
    }

    public void paint(Graphics g) {
        super.paint(g);
        Dimension s = getSize();
        int cx = s.width / 2;
        int cy = s.height / 2;
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        boolean acostado = (splitPane != null)
                && splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT;
        for (int i = -1; i <= 1; i++) {
            if (acostado) {
                g.fillRect(cx - 1, cy + i * 4 - 1, 2, 2);
            } else {
                g.fillRect(cx + i * 4 - 1, cy - 1, 2, 2);
            }
        }
    }
}
