package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalBorders;

/**
 * El aspecto basico de un boton con estado.
 *
 * <p>Difiere de {@link BasicButtonUI} en tres cosas: "apretado" incluye "seleccionado" al elegir
 * si pintar el fondo apretado, el icono seleccionado se elige antes que el de rollover, y el texto
 * nunca se corre. Los valores por omision son los de {@code ToggleButton.*} en Metal: margen
 * (2, 14, 2, 14), el borde de {@link MetalBorders#getToggleButtonBorder}, y sin rollover.
 */
public class BasicToggleButtonUI extends BasicButtonUI {

    private static final BasicToggleButtonUI toggleButtonUI = new BasicToggleButtonUI();

    private static final String propertyPrefix = "ToggleButton.";

    public BasicToggleButtonUI() {
    }

    /** El aspecto compartido. */
    public static ComponentUI createUI(JComponent b) {
        return toggleButtonUI;
    }

    protected String getPropertyPrefix() {
        return propertyPrefix;
    }

    Border bordePorOmision() {
        return MetalBorders.getToggleButtonBorder();
    }

    /** {@code ToggleButton.rollover} no esta definido en Metal: no se instala nada. */
    Boolean rolloverPorOmision() {
        return null;
    }

    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel modelo = b.getModel();
        Dimension tamano = b.getSize();
        Insets i = c.getInsets();
        Rectangle vistaR = new Rectangle(tamano);
        vistaR.x = vistaR.x + i.left;
        vistaR.y = vistaR.y + i.top;
        vistaR.width = vistaR.width - (i.right + vistaR.x);
        vistaR.height = vistaR.height - (i.bottom + vistaR.y);
        Rectangle iconoR = new Rectangle();
        Rectangle textoR = new Rectangle();

        Font f = c.getFont();
        g.setFont(f);
        FontMetrics fm = b.getFontMetrics(f);

        String texto = SwingUtilities.layoutCompoundLabel(c, fm, b.getText(), b.getIcon(),
                b.getVerticalAlignment(), b.getHorizontalAlignment(),
                b.getVerticalTextPosition(), b.getHorizontalTextPosition(), vistaR, iconoR,
                textoR, b.getText() == null ? 0 : b.getIconTextGap());

        g.setColor(b.getBackground());

        if ((modelo.isArmed() && modelo.isPressed()) || modelo.isSelected()) {
            paintButtonPressed(g, b);
        }
        if (b.getIcon() != null) {
            paintIcon(g, b, iconoR);
        }
        if (texto != null && !texto.isEmpty()) {
            paintText(g, b, textoR, texto);
        }
        if (b.isFocusPainted() && b.hasFocus()) {
            paintFocus(g, b, vistaR, textoR, iconoR);
        }
    }

    /**
     * Pinta el icono del estado: deshabilitado, apretado, seleccionado (con o sin rollover),
     * rollover, y el comun si el del estado no esta.
     */
    protected void paintIcon(Graphics g, AbstractButton b, Rectangle iconRect) {
        ButtonModel modelo = b.getModel();
        Icon icono = null;
        if (!modelo.isEnabled()) {
            if (modelo.isSelected()) {
                icono = b.getDisabledSelectedIcon();
            } else {
                icono = b.getDisabledIcon();
            }
        } else if (modelo.isPressed() && modelo.isArmed()) {
            icono = b.getPressedIcon();
            if (icono == null) {
                icono = b.getSelectedIcon();
            }
        } else if (modelo.isSelected()) {
            if (b.isRolloverEnabled() && modelo.isRollover()) {
                icono = b.getRolloverSelectedIcon();
                if (icono == null) {
                    icono = b.getSelectedIcon();
                }
            } else {
                icono = b.getSelectedIcon();
            }
        } else if (b.isRolloverEnabled() && modelo.isRollover()) {
            icono = b.getRolloverIcon();
        }
        if (icono == null) {
            icono = b.getIcon();
        }
        icono.paintIcon(b, g, iconRect.x, iconRect.y);
    }

    /** Cero: un boton con estado no corre el texto al apretarse. */
    protected int getTextShiftOffset() {
        return 0;
    }
}
