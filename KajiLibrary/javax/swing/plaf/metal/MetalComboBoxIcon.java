package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;

/**
 * La flechita de un desplegable de Metal: diez de ancho por cinco de alto.
 *
 * <p>Es un triangulo lleno que apunta para abajo, dibujado renglon por renglon: se arranca con una
 * linea de diez y se le quita uno de cada lado en cada renglon. Cinco renglones para diez pixeles
 * de ancho no es casualidad: es lo que hace que los lados queden a cuarenta y cinco grados y no se
 * vean escalonados.
 *
 * <p>No es un {@code UIResource}, y eso importa: un desplegable al que el programa le puso este
 * icono a mano se lo queda aunque cambie el aspecto. Los iconos que si son {@code UIResource} -- el
 * de la casilla, por ejemplo -- se reemplazan solos.
 */
public class MetalComboBoxIcon implements Icon, Serializable {

    public MetalComboBoxIcon() {
    }

    public int getIconWidth() {
        return 10;
    }

    public int getIconHeight() {
        return 5;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        int ancho = getIconWidth();
        g.translate(x, y);
        g.setColor((c == null || c.isEnabled())
                ? MetalLookAndFeel.getControlInfo()
                : MetalLookAndFeel.getControlShadow());
        // Renglon i: se arranca en i y se termina dos pixeles antes que el anterior.
        for (int i = 0; i < getIconHeight(); i++) {
            g.drawLine(i, i, i + (ancho - 1 - 2 * i), i);
        }
        g.translate(-x, -y);
    }
}
