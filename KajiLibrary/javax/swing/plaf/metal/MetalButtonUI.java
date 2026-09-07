package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonListener;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * El boton de Metal.
 *
 * <h2>Tres colores que el basico no tiene</h2>
 *
 * <p>{@link #focusColor} es el rectangulo punteado de adentro, {@link #selectColor} el relleno
 * mientras el boton esta apretado y {@link #disabledTextColor} el texto de uno apagado. El basico
 * no los guarda: dibuja el foco con el color de frente y el apretado oscureciendo el fondo. Metal
 * los saca del tema, y por eso un cambio de tema le cambia los tres de golpe.
 *
 * <p>Los tres campos se leen por su {@code getXxx}, y esos metodos existen para algo concreto:
 * {@link MetalToggleButtonUI} tiene los mismos tres campos y <em>no</em> hereda de esta clase --
 * hereda del conmutador basico -- asi que la unica forma de compartir el dibujo es que cada uno
 * conteste los suyos.
 *
 * <h2>Comparte instancia</h2>
 *
 * <p>{@link #createUI} devuelve siempre la misma. Los tres colores son del tema y no del boton, asi
 * que no hay nada por boton que guardar.
 */
public class MetalButtonUI extends BasicButtonUI {

    private static final MetalButtonUI UNICO = new MetalButtonUI();

    protected Color focusColor;
    protected Color selectColor;
    protected Color disabledTextColor;

    public MetalButtonUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return UNICO;
    }

    protected Color getFocusColor() {
        if (focusColor == null) {
            focusColor = MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "focus");
        }
        return focusColor;
    }

    protected Color getSelectColor() {
        if (selectColor == null) {
            selectColor = MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "select");
        }
        return selectColor;
    }

    protected Color getDisabledTextColor() {
        if (disabledTextColor == null) {
            disabledTextColor =
                    MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "disabledText");
        }
        return disabledTextColor;
    }

    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        focusColor = MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "focus");
        selectColor = MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "select");
        disabledTextColor =
                MetalLookAndFeel.colorDeLaTabla(getPropertyPrefix() + "disabledText");
    }

    public void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        // Los tres colores no se sueltan: este UI lo comparten todos los botones del
        // programa, y soltarlos al desinstalar uno dejaria a los demas sin color. Medido.
    }

    protected BasicButtonListener createButtonListener(AbstractButton b) {
        return super.createButtonListener(b);
    }

    /** El relleno del boton apretado, en el color de seleccion del tema. */
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if (b.isContentAreaFilled()) {
            Dimension s = b.getSize();
            g.setColor(getSelectColor());
            g.fillRect(0, 0, s.width, s.height);
        }
    }

    /** Un rectangulo punteado por adentro del borde. */
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
            Rectangle textRect, Rectangle iconRect) {
        Rectangle f = b.getVisibleRect();
        g.setColor(getFocusColor());
        g.drawRect(f.x + 1, f.y + 1, f.width - 3, f.height - 3);
    }

    /** El texto apagado va en el gris del tema, no en el fondo oscurecido. */
    protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel m = b.getModel();
        if (m.isEnabled()) {
            super.paintText(g, c, textRect, text);
            return;
        }
        Color antes = g.getColor();
        g.setColor(getDisabledTextColor());
        super.paintText(g, c, textRect, text);
        g.setColor(antes);
    }

    public void update(Graphics g, JComponent c) {
        super.update(g, c);
    }
}
