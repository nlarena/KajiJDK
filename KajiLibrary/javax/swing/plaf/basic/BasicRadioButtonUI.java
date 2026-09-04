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
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.metal.MetalIconFactory;

/**
 * El aspecto basico de un boton de radio: un icono por omision que pinta el estado, y el texto
 * al lado.
 *
 * <p>El icono es lo nuevo respecto de {@link BasicToggleButtonUI}: si el boton no tiene uno
 * propio, se usa {@link #getDefaultIcon}, que en Metal es el circulo de
 * {@link MetalIconFactory#getRadioButtonIcon}, y ese icono lee el modelo para saber como
 * pintarse. Un icono propio del boton se elige por estado como en cualquier boton con estado.
 *
 * <p>Los valores por omision son los de {@code RadioButton.*} en Metal: margen (2, 2, 2, 2), el
 * borde de {@link BasicBorders#getRadioButtonBorder} (que no se pinta: el boton nace con
 * {@code borderPainted} en {@code false}, pero sus insets cuentan), y rollover.
 */
public class BasicRadioButtonUI extends BasicToggleButtonUI {

    private static final BasicRadioButtonUI radioButtonUI = new BasicRadioButtonUI();

    /** El icono por omision; ver la nota de la clase. */
    protected Icon icon;

    private boolean defaults_initialized = false;

    private static final String propertyPrefix = "RadioButton.";

    public BasicRadioButtonUI() {
    }

    /** El aspecto compartido. */
    public static ComponentUI createUI(JComponent b) {
        return radioButtonUI;
    }

    protected String getPropertyPrefix() {
        return propertyPrefix;
    }

    Insets margenPorOmision() {
        return new InsetsUIResource(2, 2, 2, 2);
    }

    Border bordePorOmision() {
        return BasicBorders.getRadioButtonBorder();
    }

    Boolean rolloverPorOmision() {
        return Boolean.TRUE;
    }

    /** Lo que {@code UIManager} daria bajo {@code prefijo + "icon"}. */
    Icon iconoPorOmision() {
        return MetalIconFactory.getRadioButtonIcon();
    }

    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        if (!defaults_initialized) {
            icon = iconoPorOmision();
            defaults_initialized = true;
        }
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        defaults_initialized = false;
    }

    public Icon getDefaultIcon() {
        return icon;
    }

    public synchronized void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel modelo = b.getModel();

        Font f = c.getFont();
        g.setFont(f);
        FontMetrics fm = b.getFontMetrics(f);

        Insets i = c.getInsets();
        Dimension tamano = b.getSize();
        Rectangle vistaR = new Rectangle(i.left, i.top, tamano.width - (i.right + i.left),
                tamano.height - (i.bottom + i.top));
        Rectangle iconoR = new Rectangle();
        Rectangle textoR = new Rectangle();

        Icon propio = b.getIcon();
        String texto = SwingUtilities.layoutCompoundLabel(c, fm, b.getText(),
                propio != null ? propio : getDefaultIcon(), b.getVerticalAlignment(),
                b.getHorizontalAlignment(), b.getVerticalTextPosition(),
                b.getHorizontalTextPosition(), vistaR, iconoR, textoR,
                b.getText() == null ? 0 : b.getIconTextGap());

        if (c.isOpaque()) {
            g.setColor(b.getBackground());
            g.fillRect(0, 0, tamano.width, tamano.height);
        }

        if (propio != null) {
            Icon delEstado = propio;
            if (!modelo.isEnabled()) {
                if (modelo.isSelected()) {
                    delEstado = b.getDisabledSelectedIcon();
                } else {
                    delEstado = b.getDisabledIcon();
                }
            } else if (modelo.isPressed() && modelo.isArmed()) {
                delEstado = b.getPressedIcon();
                if (delEstado == null) {
                    delEstado = b.getSelectedIcon();
                }
            } else if (modelo.isSelected()) {
                if (b.isRolloverEnabled() && modelo.isRollover()) {
                    delEstado = b.getRolloverSelectedIcon();
                    if (delEstado == null) {
                        delEstado = b.getSelectedIcon();
                    }
                } else {
                    delEstado = b.getSelectedIcon();
                }
            } else if (b.isRolloverEnabled() && modelo.isRollover()) {
                delEstado = b.getRolloverIcon();
            }
            if (delEstado == null) {
                delEstado = b.getIcon();
            }
            delEstado.paintIcon(c, g, iconoR.x, iconoR.y);
        } else {
            getDefaultIcon().paintIcon(c, g, iconoR.x, iconoR.y);
        }

        if (texto != null) {
            paintText(g, b, textoR, texto);
            if (b.hasFocus() && b.isFocusPainted() && textoR.width > 0 && textoR.height > 0) {
                paintFocus(g, textoR, tamano);
            }
        }
    }

    /** Nada: el aspecto basico no marca el foco; los que derivan de el, si. */
    protected void paintFocus(Graphics g, Rectangle textRect, Dimension size) {
    }

    /** Icono y texto en una vista infinita, mas los insets; {@code null} si el boton tiene hijos. */
    public Dimension getPreferredSize(JComponent c) {
        if (c.getComponentCount() > 0) {
            return null;
        }
        AbstractButton b = (AbstractButton) c;
        String texto = b.getText();
        Icon icono = b.getIcon();
        if (icono == null) {
            icono = getDefaultIcon();
        }
        Font fuente = b.getFont();
        FontMetrics fm = b.getFontMetrics(fuente);

        Rectangle vistaR = new Rectangle(0, 0, Short.MAX_VALUE, Short.MAX_VALUE);
        Rectangle iconoR = new Rectangle();
        Rectangle textoR = new Rectangle();
        SwingUtilities.layoutCompoundLabel(c, fm, texto, icono, b.getVerticalAlignment(),
                b.getHorizontalAlignment(), b.getVerticalTextPosition(),
                b.getHorizontalTextPosition(), vistaR, iconoR, textoR,
                texto == null ? 0 : b.getIconTextGap());

        int x1 = Math.min(iconoR.x, textoR.x);
        int x2 = Math.max(iconoR.x + iconoR.width, textoR.x + textoR.width);
        int y1 = Math.min(iconoR.y, textoR.y);
        int y2 = Math.max(iconoR.y + iconoR.height, textoR.y + textoR.height);
        int ancho = x2 - x1;
        int alto = y2 - y1;

        Insets insets = b.getInsets();
        ancho = ancho + insets.left + insets.right;
        alto = alto + insets.top + insets.bottom;
        return new Dimension(ancho, alto);
    }
}
