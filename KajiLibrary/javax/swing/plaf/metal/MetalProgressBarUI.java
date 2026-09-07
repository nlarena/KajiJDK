package javax.swing.plaf.metal;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

/**
 * La barra de progreso de Metal.
 *
 * <p>Lo unico que agrega al basico es una linea de sombra: donde termina la parte llena se dibuja
 * un borde del color oscuro del tema. Es lo que evita que el relleno y el fondo se toquen sin nada
 * en el medio, que a mitad de camino se lee como una mancha y no como un progreso.
 *
 * <p>Los dos metodos son el mismo dibujo para los dos modos: el que sabe cuanto falta y el que
 * anda de un lado al otro sin saberlo.
 */
public class MetalProgressBarUI extends BasicProgressBarUI {

    public MetalProgressBarUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalProgressBarUI();
    }

    public void paintDeterminate(Graphics g, JComponent c) {
        super.paintDeterminate(g, c);
        sombra(g, c, cuantoLleva(c));
    }

    public void paintIndeterminate(Graphics g, JComponent c) {
        super.paintIndeterminate(g, c);
        Rectangle caja = getBox(null);
        if (caja != null) {
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(caja.x, caja.y, caja.width - 1, caja.height - 1);
        }
    }

    /** La linea donde termina lo lleno. */
    private void sombra(Graphics g, JComponent c, int lleno) {
        if (lleno <= 0) {
            return;
        }
        JProgressBar b = (JProgressBar) c;
        Insets i = b.getInsets();
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        if (b.getOrientation() == JProgressBar.HORIZONTAL) {
            int alto = b.getHeight() - i.top - i.bottom;
            g.drawRect(i.left, i.top, lleno - 1, alto - 1);
        } else {
            int ancho = b.getWidth() - i.left - i.right;
            int alto = b.getHeight() - i.top - i.bottom;
            g.drawRect(i.left, i.top + alto - lleno, ancho - 1, lleno - 1);
        }
    }

    /** Los pixeles llenos, a lo largo de la barra. */
    private int cuantoLleva(JComponent c) {
        JProgressBar b = (JProgressBar) c;
        Insets i = b.getInsets();
        int largo = (b.getOrientation() == JProgressBar.HORIZONTAL)
                ? b.getWidth() - i.left - i.right
                : b.getHeight() - i.top - i.bottom;
        return getAmountFull(i, largo, largo);
    }
}
