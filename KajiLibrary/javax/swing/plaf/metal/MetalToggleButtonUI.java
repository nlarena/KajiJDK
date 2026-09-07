package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

/**
 * El conmutador de Metal.
 *
 * <p>Es el gemelo de {@link MetalButtonUI} y no comparte una linea de codigo con el, porque los dos
 * heredan de distinto lado: uno del boton basico y este del conmutador basico. Java no tiene
 * herencia multiple y esta es la clase donde mas se nota: los tres campos de color, los tres
 * {@code getXxx}, {@code installDefaults} y {@code paintText} estan escritos dos veces, iguales.
 *
 * <p>Lo unico que agrega de verdad es {@link #paintIcon}: un conmutador con icono y elegido dibuja
 * el icono sobre el color de seleccion, cosa que un boton comun no necesita porque no se queda
 * elegido.
 */
public class MetalToggleButtonUI extends BasicToggleButtonUI {

    private static final MetalToggleButtonUI UNICO = new MetalToggleButtonUI();

    protected Color focusColor;
    protected Color selectColor;
    protected Color disabledTextColor;

    public MetalToggleButtonUI() {
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

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        // Los tres colores no se sueltan: este UI lo comparten todos los botones del
        // programa, y soltarlos al desinstalar uno dejaria a los demas sin color. Medido.
    }

    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if (b.isContentAreaFilled()) {
            Dimension s = b.getSize();
            g.setColor(getSelectColor());
            g.fillRect(0, 0, s.width, s.height);
        }
    }

    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
            Rectangle textRect, Rectangle iconRect) {
        Rectangle f = b.getVisibleRect();
        g.setColor(getFocusColor());
        g.drawRect(f.x + 1, f.y + 1, f.width - 3, f.height - 3);
    }

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

    /** Un conmutador elegido lleva el icono sobre el color de seleccion. */
    protected void paintIcon(Graphics g, AbstractButton b, Rectangle iconRect) {
        ButtonModel m = b.getModel();
        if (m.isSelected() && !m.isArmed() && b.isContentAreaFilled() && b.isOpaque()) {
            g.setColor(getSelectColor());
            g.fillRect(iconRect.x, iconRect.y, iconRect.width, iconRect.height);
        }
        super.paintIcon(g, b, iconRect);
    }

    public void update(Graphics g, JComponent c) {
        super.update(g, c);
    }
}
