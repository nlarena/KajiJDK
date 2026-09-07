package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSeparatorUI;

/**
 * El separador de Metal: dos pixeles y dos lineas.
 *
 * <p>La linea de arriba va en el color de frente del tema y la de abajo en blanco. Ese par -- una
 * oscura y una clara, en ese orden -- es lo que hace que se lea como un surco y no como una raya:
 * la luz viene de arriba a la izquierda, asi que el borde superior de una hendidura esta en sombra
 * y el inferior recibe luz.
 *
 * <p>Por eso el tamano preferido son dos pixeles y no uno, y por eso el separador de Metal se ve
 * distinto al basico aunque ninguno de los dos dibuje mas que dos lineas.
 */
public class MetalSeparatorUI extends BasicSeparatorUI {

    public MetalSeparatorUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalSeparatorUI();
    }

    /** Blanco abajo y el color del tema arriba; ver la nota de la clase. */
    protected void installDefaults(JSeparator s) {
        s.setBackground(MetalLookAndFeel.getSeparatorBackground());
        s.setForeground(MetalLookAndFeel.getSeparatorForeground());
    }

    public void paint(Graphics g, JComponent c) {
        Dimension s = c.getSize();
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            g.setColor(c.getForeground());
            g.drawLine(0, 0, 0, s.height);
            g.setColor(c.getBackground());
            g.drawLine(1, 0, 1, s.height);
        } else {
            g.setColor(c.getForeground());
            g.drawLine(0, 0, s.width, 0);
            g.setColor(c.getBackground());
            g.drawLine(0, 1, s.width, 1);
        }
    }

    public Dimension getPreferredSize(JComponent c) {
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(2, 0);
        }
        return new Dimension(0, 2);
    }
}
