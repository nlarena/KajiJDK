package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.LabelUI;

/**
 * El aspecto basico de una etiqueta: ubica texto e icono y los pinta.
 *
 * <h2>Un solo objeto para todas las etiquetas</h2>
 *
 * <p>{@link #createUI} devuelve siempre la misma instancia, y puede porque este UI
 * <strong>no guarda nada del componente</strong>: cada metodo recibe la etiqueta y trabaja sobre
 * ella. Es la razon de que los rectangulos de trabajo sean locales y no campos — un campo
 * compartido entre mil etiquetas seria un dato de la ultima que se pinto.
 *
 * <h2>Lo que instala, y de donde salen los valores</h2>
 *
 * <p>{@link #installDefaults} pone la fuente y los colores que en el JDK vienen de
 * {@code UIManager} bajo {@code Label.font}, {@code Label.foreground} y
 * {@code Label.background}. Sin {@code UIManager}, son los valores <em>medidos</em> en el aspecto
 * Metal del JDK 25: Dialog negrita 12, gris (51, 51, 51) y (238, 238, 238). Se instalan solo donde
 * el componente no tiene nada: el JDK distingue "lo puso el usuario" de "lo puso un aspecto" con
 * las clases {@code UIResource}, que no estan, y la aproximacion honesta es no pisar lo que ya
 * habia.
 *
 * <p>La negrita es la de la API: el rasterizador de esta VM dibuja toda fuente con la misma cara
 * regular, asi que el texto sale regular aunque {@code getFont} diga negrita. Es la sustitucion de
 * {@code jdk.internal.awt.FuenteBitmap}, dicha en cada lugar donde se nota.
 */
public class BasicLabelUI extends LabelUI implements PropertyChangeListener {

    /** La instancia compartida; ver la nota de la clase. */
    protected static BasicLabelUI labelUI = new BasicLabelUI();

    private static final Font FUENTE_POR_OMISION = new Font("Dialog", Font.BOLD, 12);
    private static final Color FRENTE_POR_OMISION = new Color(51, 51, 51);
    private static final Color FONDO_POR_OMISION = new Color(238, 238, 238);

    /** Un aspecto nuevo. Lo normal es pedir el de {@link #createUI}. */
    public BasicLabelUI() {
    }

    /** El aspecto compartido. */
    public static ComponentUI createUI(JComponent c) {
        return labelUI;
    }

    /**
     * Ubica texto e icono; devuelve el texto, posiblemente recortado.
     *
     * <p>Delegar en {@link SwingUtilities#layoutCompoundLabel} es lo que hace que una etiqueta y un
     * boton coloquen su texto igual: es un solo algoritmo con dos llamadores.
     */
    protected String layoutCL(JLabel label, FontMetrics fontMetrics, String text, Icon icon,
            Rectangle viewR, Rectangle iconR, Rectangle textR) {
        return SwingUtilities.layoutCompoundLabel(label, fontMetrics, text, icon,
                label.getVerticalAlignment(), label.getHorizontalAlignment(),
                label.getVerticalTextPosition(), label.getHorizontalTextPosition(),
                viewR, iconR, textR, label.getIconTextGap());
    }

    /** Pinta el texto de una etiqueta habilitada, con su mnemonico subrayado. */
    protected void paintEnabledText(JLabel l, Graphics g, String s, int textX, int textY) {
        int indice = l.getDisplayedMnemonicIndex();
        g.setColor(l.getForeground());
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, s, indice, textX, textY);
    }

    /**
     * Pinta el texto de una etiqueta deshabilitada, en relieve.
     *
     * <p>Dos pasadas: el fondo aclarado un pixel abajo y a la derecha, y el fondo oscurecido en su
     * lugar. El texto queda como grabado en el fondo, que es como el aspecto basico dice "esto no
     * responde".
     */
    protected void paintDisabledText(JLabel l, Graphics g, String s, int textX, int textY) {
        int indice = l.getDisplayedMnemonicIndex();
        Color fondo = l.getBackground();
        g.setColor(fondo.brighter());
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, s, indice, textX + 1, textY + 1);
        g.setColor(fondo.darker());
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, s, indice, textX, textY);
    }

    /**
     * Pinta icono y texto.
     *
     * <p>El fondo no se pinta aca: lo pinta {@link #update} si la etiqueta es opaca, que por omision
     * no lo es. De ahi que una etiqueta sobre un panel de otro color se vea transparente.
     */
    public void paint(Graphics g, JComponent c) {
        JLabel label = (JLabel) c;
        String texto = label.getText();
        Icon icono = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();
        if (icono == null && texto == null) {
            return;
        }
        FontMetrics fm = label.getFontMetrics(label.getFont());
        Insets insets = c.getInsets(new Insets(0, 0, 0, 0));
        Rectangle vistaR = new Rectangle(insets.left, insets.top,
                c.getWidth() - (insets.left + insets.right),
                c.getHeight() - (insets.top + insets.bottom));
        Rectangle iconoR = new Rectangle();
        Rectangle textoR = new Rectangle();
        String recortado = layoutCL(label, fm, texto, icono, vistaR, iconoR, textoR);
        if (icono != null) {
            icono.paintIcon(c, g, iconoR.x, iconoR.y);
        }
        if (texto != null) {
            int textoX = textoR.x;
            int textoY = textoR.y + fm.getAscent();
            if (label.isEnabled()) {
                paintEnabledText(label, g, recortado, textoX, textoY);
            } else {
                paintDisabledText(label, g, recortado, textoX, textoY);
            }
        }
    }

    /**
     * El tamano preferido: lo que ocupan texto e icono ubicados en una vista infinita, mas los
     * insets.
     *
     * <p>La vista infinita es el truco: sin limite de ancho nada se recorta, y la caja que queda es
     * el tamano natural de la etiqueta.
     */
    public Dimension getPreferredSize(JComponent c) {
        JLabel label = (JLabel) c;
        String texto = label.getText();
        Icon icono = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();
        Insets insets = label.getInsets(new Insets(0, 0, 0, 0));
        Font fuente = label.getFont();
        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;

        if (icono == null && (texto == null || fuente == null)) {
            return new Dimension(dx, dy);
        }
        if (texto == null || (icono != null && fuente == null)) {
            return new Dimension(icono.getIconWidth() + dx, icono.getIconHeight() + dy);
        }
        FontMetrics fm = label.getFontMetrics(fuente);
        Rectangle iconoR = new Rectangle();
        Rectangle textoR = new Rectangle();
        Rectangle vistaR = new Rectangle(dx, dy, Short.MAX_VALUE, Short.MAX_VALUE);
        layoutCL(label, fm, texto, icono, vistaR, iconoR, textoR);
        int x1 = Math.min(iconoR.x, textoR.x);
        int x2 = Math.max(iconoR.x + iconoR.width, textoR.x + textoR.width);
        int y1 = Math.min(iconoR.y, textoR.y);
        int y2 = Math.max(iconoR.y + iconoR.height, textoR.y + textoR.height);
        Dimension rv = new Dimension(x2 - x1, y2 - y1);
        rv.width = rv.width + dx;
        rv.height = rv.height + dy;
        return rv;
    }

    /** El minimo es el preferido: una etiqueta no se achica sin recortar. */
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** El maximo es el preferido: una etiqueta no crece por si sola. */
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** La linea de base del texto, ubicado en esa caja; {@code -1} sin texto. */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        JLabel label = (JLabel) c;
        String texto = label.getText();
        if (texto == null || texto.isEmpty() || label.getFont() == null) {
            return -1;
        }
        FontMetrics fm = label.getFontMetrics(label.getFont());
        Insets insets = label.getInsets(new Insets(0, 0, 0, 0));
        Rectangle vistaR = new Rectangle(insets.left, insets.top,
                width - (insets.left + insets.right), height - (insets.top + insets.bottom));
        Rectangle iconoR = new Rectangle();
        Rectangle textoR = new Rectangle();
        layoutCL(label, fm, texto, label.isEnabled() ? label.getIcon() : label.getDisabledIcon(),
                vistaR, iconoR, textoR);
        return textoR.y + fm.getAscent();
    }

    /** Como se mueve la linea de base: segun donde este alineado el texto verticalmente. */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        JLabel label = (JLabel) c;
        int v = label.getVerticalAlignment();
        if (v == JLabel.TOP) {
            return Component$BaselineResizeBehavior.CONSTANT_ASCENT;
        }
        if (v == JLabel.BOTTOM) {
            return Component$BaselineResizeBehavior.CONSTANT_DESCENT;
        }
        if (v == JLabel.CENTER) {
            return Component$BaselineResizeBehavior.CENTER_OFFSET;
        }
        return Component$BaselineResizeBehavior.OTHER;
    }

    public void installUI(JComponent c) {
        JLabel label = (JLabel) c;
        installDefaults(label);
        installComponents(label);
        installListeners(label);
        installKeyboardActions(label);
    }

    public void uninstallUI(JComponent c) {
        JLabel label = (JLabel) c;
        uninstallDefaults(label);
        uninstallComponents(label);
        uninstallListeners(label);
        uninstallKeyboardActions(label);
    }

    /** Fuente y colores por omision, solo donde no hay nada; ver la nota de la clase. */
    protected void installDefaults(JLabel c) {
        if (c.getFont() == null) {
            c.setFont(FUENTE_POR_OMISION);
        }
        if (c.getForeground() == null) {
            c.setForeground(FRENTE_POR_OMISION);
        }
        if (c.getBackground() == null) {
            c.setBackground(FONDO_POR_OMISION);
        }
    }

    /** Este UI escucha los cambios de propiedad de la etiqueta. */
    protected void installListeners(JLabel c) {
        c.addPropertyChangeListener(this);
    }

    /** Una etiqueta no tiene subcomponentes; nada que instalar. */
    protected void installComponents(JLabel c) {
    }

    /**
     * Nada: las acciones por teclado de una etiqueta —darle el foco a su {@code labelFor} con el
     * mnemonico— necesitan {@code InputMap} y {@code ActionMap}, que no estan.
     */
    protected void installKeyboardActions(JLabel l) {
    }

    /** Lo instalado quedo en el componente y puede seguir ahi; el JDK tampoco lo borra. */
    protected void uninstallDefaults(JLabel c) {
    }

    protected void uninstallListeners(JLabel c) {
        c.removePropertyChangeListener(this);
    }

    protected void uninstallComponents(JLabel c) {
    }

    protected void uninstallKeyboardActions(JLabel c) {
    }

    /**
     * Cambio una propiedad de la etiqueta.
     *
     * <p>El JDK usa esto para renovar las acciones por teclado cuando cambian el texto, el
     * mnemonico o el {@code labelFor}. Sin acciones por teclado no hay nada que renovar: el
     * repintado y el relayout ya los pide la propia etiqueta al cambiar.
     */
    public void propertyChange(PropertyChangeEvent e) {
    }
}
