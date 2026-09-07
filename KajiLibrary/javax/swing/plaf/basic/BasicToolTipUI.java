package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.LookAndFeel;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ToolTipUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.View;

/**
 * El aspecto basico de un cartel de ayuda: el texto sobre un rectangulo con borde de una linea.
 *
 * <h2>Los seis pixeles</h2>
 *
 * <p>El ancho preferido es los margenes mas el ancho del texto <em>mas seis</em>, y el texto se
 * dibuja tres pixeles a la derecha del margen izquierdo. Ese seis no sale de ninguna propiedad: es
 * una constante del JDK, y es lo que hace que el texto no quede pegado al borde. Esta medido: un
 * cartel con "hola" en Dialog 12 mide 32 x 18, y uno sin texto mide 2 x 2 --solo el borde--.
 *
 * <p>Un cartel sin texto no reserva alto de linea: {@link #getPreferredSize} solo suma la altura de
 * la fuente cuando hay algo que escribir.
 *
 * <h2>Los tres tamanos son el mismo</h2>
 *
 * <p>Minimo, preferido y maximo dan lo mismo. Un cartel de ayuda no se estira ni se achica: lo
 * pone la ventana emergente del tamano que pida y listo.
 *
 * <h2>Lo que instala</h2>
 *
 * <p>Los valores de {@code ToolTip.*} medidos en Metal (JDK 25): fondo (184, 207, 229), frente
 * (51, 51, 51), Dialog 12 y un borde de linea de un pixel en (99, 130, 191). El cartel queda opaco.
 *
 * <h2>Texto con etiquetas</h2>
 *
 * <p>Si el texto es HTML --ver {@link BasicHTML#isHTMLString}-- el cartel guarda una vista armada y
 * es esa la que mide y pinta. {@link #installUI} y el cambio de texto la actualizan.
 */
public class BasicToolTipUI extends ToolTipUI {

    private static BasicToolTipUI sharedInstance = new BasicToolTipUI();

    private static final ColorUIResource FONDO_POR_OMISION = new ColorUIResource(184, 207, 229);
    private static final ColorUIResource FRENTE_POR_OMISION = new ColorUIResource(51, 51, 51);
    private static final Font FUENTE_POR_OMISION = new FontUIResource("Dialog", Font.PLAIN, 12);
    private static final Border BORDE_POR_OMISION =
            new BorderUIResource.LineBorderUIResource(new ColorUIResource(99, 130, 191), 1);

    public BasicToolTipUI() {
        super();
    }

    /** El aspecto compartido: no guarda nada del cartel. */
    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public void installUI(JComponent c) {
        installDefaults(c);
        installComponents(c);
        installListeners(c);
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults(c);
        uninstallComponents(c);
        uninstallListeners(c);
    }

    /** Colores, fuente, borde y opacidad; ver la nota de la clase. */
    protected void installDefaults(JComponent c) {
        Color fondo = c.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            c.setBackground(FONDO_POR_OMISION);
        }
        Color frente = c.getForeground();
        if (frente == null || frente instanceof UIResource) {
            c.setForeground(FRENTE_POR_OMISION);
        }
        Font fuente = c.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            c.setFont(FUENTE_POR_OMISION);
        }
        Border borde = c.getBorder();
        if (borde == null || borde instanceof UIResource) {
            c.setBorder(BORDE_POR_OMISION);
        }
        LookAndFeel.installProperty(c, "opaque", Boolean.TRUE);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults(JComponent c) {
    }

    /** Arma la vista si el texto es HTML; ver la nota de la clase. */
    private void installComponents(JComponent c) {
        BasicHTML.updateRenderer(c, ((JToolTip) c).getTipText());
    }

    private void uninstallComponents(JComponent c) {
        BasicHTML.updateRenderer(c, "");
    }

    /** No escucha nada: el cartel lo maneja {@code ToolTipManager}. */
    protected void installListeners(JComponent c) {
    }

    /** Idem. */
    protected void uninstallListeners(JComponent c) {
    }

    /** El texto, tres pixeles a la derecha del margen; ver la nota de la clase. */
    public void paint(Graphics g, JComponent c) {
        Font font = c.getFont();
        FontMetrics metrics = c.getFontMetrics(font);
        Dimension size = c.getSize();

        g.setColor(c.getForeground());
        String tipText = ((JToolTip) c).getTipText();
        if (tipText == null) {
            tipText = "";
        }

        Insets insets = c.getInsets();
        Rectangle paintTextR = new Rectangle(
                insets.left + 3,
                insets.top,
                size.width - (insets.left + insets.right) - 6,
                size.height - (insets.top + insets.bottom));
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            v.paint(g, paintTextR);
        } else {
            g.setFont(font);
            g.drawString(tipText, paintTextR.x, paintTextR.y + metrics.getAscent());
        }
    }

    /** Margenes mas el texto mas seis; ver la nota de la clase. */
    public Dimension getPreferredSize(JComponent c) {
        Font font = c.getFont();
        FontMetrics fm = c.getFontMetrics(font);
        Insets insets = c.getInsets();

        Dimension prefSize = new Dimension(insets.left + insets.right,
                insets.top + insets.bottom);
        String text = ((JToolTip) c).getTipText();

        if (text != null && !text.equals("")) {
            View v = (View) c.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                prefSize.width += (int) v.getPreferredSpan(View.X_AXIS);
                prefSize.height += (int) v.getPreferredSpan(View.Y_AXIS);
            } else {
                prefSize.width += fm.stringWidth(text) + 6;
                prefSize.height += fm.getHeight();
            }
        }
        return prefSize;
    }

    /** El mismo que el preferido; ver la nota de la clase. */
    public Dimension getMinimumSize(JComponent c) {
        Dimension d = getPreferredSize(c);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d.width -= v.getPreferredSpan(View.X_AXIS) - v.getMinimumSpan(View.X_AXIS);
        }
        return d;
    }

    /** El mismo que el preferido; ver la nota de la clase. */
    public Dimension getMaximumSize(JComponent c) {
        Dimension d = getPreferredSize(c);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d.width += v.getMaximumSpan(View.X_AXIS) - v.getPreferredSpan(View.X_AXIS);
        }
        return d;
    }
}
