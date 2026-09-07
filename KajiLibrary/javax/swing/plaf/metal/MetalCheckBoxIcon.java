package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

/**
 * El cuadradito de una casilla de Metal: trece por trece.
 *
 * <p>El tamano sale de {@link #getControlSize}, que es {@code protected} a proposito: un tema que
 * quiera casillas mas grandes hereda y cambia ese numero, y el resto -- el marco, la tilde, los
 * cuatro estados -- sigue funcionando.
 *
 * <p>La tilde tambien esta separada, en {@link #drawCheck}, por la misma razon: es la parte que un
 * tema podria querer dibujar distinta -- una cruz, un punto -- sin tocar el marco.
 *
 * <p>El icono dibuja cuatro estados y no dos: prendido y apagado, cada uno habilitado o no. Un
 * cuadrado apagado no es un cuadrado gris; es un cuadrado <em>sin marco hundido</em>, porque lo que
 * comunica que algo no responde es que deje de tener relieve.
 */
public class MetalCheckBoxIcon implements Icon, UIResource, Serializable {

    public MetalCheckBoxIcon() {
    }

    /** Trece; ver la nota de la clase. */
    protected int getControlSize() {
        return 13;
    }

    public int getIconWidth() {
        return getControlSize();
    }

    public int getIconHeight() {
        return getControlSize();
    }

    /** La tilde: dos trazos, el corto para abajo y el largo para arriba. */
    protected void drawCheck(Component c, Graphics g, int x, int y) {
        int lado = getControlSize();
        g.fillRect(x + 3, y + 5, 2, lado - 8);
        g.drawLine(x + (lado - 4), y + 3, x + 5, y + (lado - 6));
        g.drawLine(x + (lado - 4), y + 4, x + 5, y + (lado - 5));
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        int lado = getControlSize();
        ButtonModel m = (c instanceof AbstractButton)
                ? ((AbstractButton) c).getModel() : null;
        boolean prendido = (m != null) && m.isSelected();
        boolean anda = (m == null) || m.isEnabled();
        boolean apretado = (m != null) && m.isPressed() && m.isArmed();

        if (anda) {
            // El marco hundido: oscuro arriba y a la izquierda, claro abajo y a la derecha.
            g.setColor(apretado ? MetalLookAndFeel.getControlShadow()
                    : MetalLookAndFeel.getControlDarkShadow());
            g.drawLine(x, y, x + lado - 1, y);
            g.drawLine(x, y, x, y + lado - 1);
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawLine(x + lado - 1, y, x + lado - 1, y + lado - 1);
            g.drawLine(x, y + lado - 1, x + lado - 1, y + lado - 1);
            g.setColor(MetalLookAndFeel.getControlInfo());
        } else {
            // Sin relieve; ver la nota de la clase.
            g.setColor(MetalLookAndFeel.getControlShadow());
            g.drawRect(x, y, lado - 1, lado - 1);
        }
        if (prendido) {
            drawCheck(c, g, x, y);
        }
    }
}
