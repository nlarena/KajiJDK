package javax.swing.plaf.basic;

import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseMotionListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalBorders;

/**
 * El aspecto basico de un boton: ubica icono y texto con {@code layoutCompoundLabel} y los pinta
 * segun el modelo.
 *
 * <h2>Un solo objeto para todos los botones</h2>
 *
 * <p>{@link #createUI} devuelve siempre la misma instancia; puede porque el UI no guarda nada del
 * boton salvo el corrimiento del texto al apretar, que se pone y se borra dentro de un mismo
 * {@link #paint}. Los rectangulos de trabajo son locales por lo mismo.
 *
 * <h2>Lo que instala, y de donde sale</h2>
 *
 * <p>{@link #installDefaults} pone lo que en el JDK viene de {@code UIManager} bajo
 * {@code Button.*}, con los valores medidos en Metal (JDK 25): fuente Dialog negrita 12, frente
 * (51, 51, 51), fondo (238, 238, 238), margen (2, 14, 2, 14), separacion icono-texto 4, rollover
 * habilitado, corrimiento del texto 0, y el borde de {@link MetalBorders#getButtonBorder}. Se
 * instalan como {@link UIResource}, y solo donde el boton tiene nada o un valor de aspecto: lo que
 * puso el usuario se respeta, que es la regla del JDK.
 *
 * <p>La negrita es la de la API; el rasterizador dibuja toda fuente con la misma cara regular
 * ({@code jdk.internal.awt.FuenteBitmap}).
 *
 * <p>Lo que Metal pinta de mas —el degradado del fondo, el marco de foco, el fondo de seleccion al
 * apretar, el texto deshabilitado en gris plano— es de {@code MetalButtonUI}, que no esta. Este es
 * el aspecto basico tal cual: fondo plano, sin marco de foco, texto deshabilitado en relieve.
 */
public class BasicButtonUI extends ButtonUI {

    private static final BasicButtonUI buttonUI = new BasicButtonUI();

    private static final Font FUENTE_POR_OMISION = new FontUIResource("Dialog", Font.BOLD, 12);
    private static final ColorUIResource FRENTE_POR_OMISION = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource FONDO_POR_OMISION = new ColorUIResource(238, 238, 238);

    /** La separacion entre icono y texto que devuelve {@link #getDefaultTextIconGap}. */
    protected int defaultTextIconGap;

    private int corrimiento = 0;

    /** Cuanto se corre el texto al apretar; cero en Metal. */
    protected int defaultTextShiftOffset;

    public BasicButtonUI() {
    }

    /** El aspecto compartido. */
    public static ComponentUI createUI(JComponent c) {
        return buttonUI;
    }

    /** El prefijo de las claves de {@code UIManager} de este componente. */
    protected String getPropertyPrefix() {
        return "Button.";
    }

    public void installUI(JComponent c) {
        AbstractButton b = (AbstractButton) c;
        installDefaults(b);
        installListeners(b);
        installKeyboardActions(b);
    }

    /** Ver la nota de la clase. */
    protected void installDefaults(AbstractButton b) {
        defaultTextShiftOffset = 0;

        if (b.isContentAreaFilled()) {
            LookAndFeel.installProperty(b, "opaque", Boolean.TRUE);
        } else {
            LookAndFeel.installProperty(b, "opaque", Boolean.FALSE);
        }

        if (b.getMargin() == null || (b.getMargin() instanceof UIResource)) {
            b.setMargin(margenPorOmision());
        }
        if (b.getBackground() == null || (b.getBackground() instanceof UIResource)) {
            b.setBackground(FONDO_POR_OMISION);
        }
        if (b.getForeground() == null || (b.getForeground() instanceof UIResource)) {
            b.setForeground(FRENTE_POR_OMISION);
        }
        if (b.getFont() == null || (b.getFont() instanceof UIResource)) {
            b.setFont(FUENTE_POR_OMISION);
        }
        if (b.getBorder() == null || (b.getBorder() instanceof UIResource)) {
            b.setBorder(bordePorOmision());
        }
        Boolean rollover = rolloverPorOmision();
        if (rollover != null) {
            LookAndFeel.installProperty(b, "rolloverEnabled", rollover);
        }
        LookAndFeel.installProperty(b, "iconTextGap", Integer.valueOf(4));
    }

    /**
     * Lo que {@code UIManager} daria bajo {@code prefijo + "margin"}: (2, 14, 2, 14) para un boton.
     * Las subclases responden por su prefijo.
     */
    Insets margenPorOmision() {
        return new InsetsUIResource(2, 14, 2, 14);
    }

    /** Lo que {@code UIManager} daria bajo {@code prefijo + "border"}. */
    Border bordePorOmision() {
        return MetalBorders.getButtonBorder();
    }

    /**
     * Lo que {@code UIManager} daria bajo {@code prefijo + "rollover"}; {@code null} si no hay
     * valor, y entonces no se instala nada. Metal lo define para todos menos el boton con estado.
     */
    Boolean rolloverPorOmision() {
        return Boolean.TRUE;
    }

    protected void installListeners(AbstractButton b) {
        BasicButtonListener escucha = createButtonListener(b);
        if (escucha != null) {
            b.addMouseListener(escucha);
            b.addMouseMotionListener(escucha);
            b.addFocusListener(escucha);
            b.addPropertyChangeListener(escucha);
            b.addChangeListener(escucha);
        }
    }

    protected void installKeyboardActions(AbstractButton b) {
        BasicButtonListener escucha = escuchaDe(b);
        if (escucha != null) {
            escucha.installKeyboardActions(b);
        }
    }

    public void uninstallUI(JComponent c) {
        uninstallKeyboardActions((AbstractButton) c);
        uninstallListeners((AbstractButton) c);
        uninstallDefaults((AbstractButton) c);
    }

    protected void uninstallKeyboardActions(AbstractButton b) {
        BasicButtonListener escucha = escuchaDe(b);
        if (escucha != null) {
            escucha.uninstallKeyboardActions(b);
        }
    }

    protected void uninstallListeners(AbstractButton b) {
        BasicButtonListener escucha = escuchaDe(b);
        if (escucha != null) {
            b.removeMouseListener(escucha);
            b.removeMouseMotionListener(escucha);
            b.removeFocusListener(escucha);
            b.removeChangeListener(escucha);
            b.removePropertyChangeListener(escucha);
        }
    }

    /** Quita el borde si es del aspecto; los colores y la fuente se quedan, como en el JDK. */
    protected void uninstallDefaults(AbstractButton b) {
        LookAndFeel.uninstallBorder(b);
    }

    protected BasicButtonListener createButtonListener(AbstractButton b) {
        return new BasicButtonListener(b);
    }

    /** El escucha que este UI instalo, buscandolo entre los del mouse; {@code null} si no hay. */
    private BasicButtonListener escuchaDe(AbstractButton b) {
        MouseMotionListener[] escuchas = b.getMouseMotionListeners();
        if (escuchas != null) {
            for (int i = 0; i < escuchas.length; i++) {
                if (escuchas[i] instanceof BasicButtonListener) {
                    return (BasicButtonListener) escuchas[i];
                }
            }
        }
        return null;
    }

    public int getDefaultTextIconGap(AbstractButton b) {
        return defaultTextIconGap;
    }

    /**
     * Ubica icono y texto en el boton de ese tamano; devuelve el texto, recortado si no entra.
     *
     * <p>Sin texto, la separacion icono-texto es cero: un boton de solo icono lo centra sin dejar
     * lugar para un texto que no esta.
     */
    private String ubicar(AbstractButton b, FontMetrics fm, int ancho, int alto,
            Rectangle vistaR, Rectangle iconoR, Rectangle textoR) {
        Insets i = b.getInsets();
        vistaR.x = i.left;
        vistaR.y = i.top;
        vistaR.width = ancho - (i.right + vistaR.x);
        vistaR.height = alto - (i.bottom + vistaR.y);
        textoR.x = 0;
        textoR.y = 0;
        textoR.width = 0;
        textoR.height = 0;
        iconoR.x = 0;
        iconoR.y = 0;
        iconoR.width = 0;
        iconoR.height = 0;
        return SwingUtilities.layoutCompoundLabel(b, fm, b.getText(), b.getIcon(),
                b.getVerticalAlignment(), b.getHorizontalAlignment(),
                b.getVerticalTextPosition(), b.getHorizontalTextPosition(), vistaR, iconoR,
                textoR, b.getText() == null ? 0 : b.getIconTextGap());
    }

    /**
     * Pinta el boton: lo apretado, el icono, el texto y el foco, en ese orden.
     *
     * <p>El fondo no se pinta aca sino en {@link #update}, si el boton es opaco; y el borde lo
     * pinta el propio boton despues, si {@code isBorderPainted}.
     */
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel modelo = b.getModel();
        FontMetrics fm = b.getFontMetrics(b.getFont());
        Rectangle vistaR = new Rectangle();
        Rectangle iconoR = new Rectangle();
        Rectangle textoR = new Rectangle();
        String texto = ubicar(b, fm, b.getWidth(), b.getHeight(), vistaR, iconoR, textoR);

        clearTextShiftOffset();

        if (modelo.isArmed() && modelo.isPressed()) {
            paintButtonPressed(g, b);
        }
        if (b.getIcon() != null) {
            paintIcon(g, c, iconoR);
        }
        if (texto != null && !texto.isEmpty()) {
            paintText(g, b, textoR, texto);
        }
        if (b.isFocusPainted() && b.hasFocus()) {
            paintFocus(g, b, vistaR, textoR, iconoR);
        }
    }

    /**
     * Pinta el icono que corresponde al estado.
     *
     * <p>La eleccion va de mas a menos especifico: deshabilitado (y seleccionado), apretado,
     * rollover (y seleccionado), seleccionado, y el icono comun si el del estado no esta. Un
     * icono apretado que existe anula el corrimiento del texto: ya dice "apretado" el solo.
     */
    protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel modelo = b.getModel();
        Icon icono = b.getIcon();
        Icon delEstado = null;
        if (icono == null) {
            return;
        }
        if (modelo.isSelected()) {
            Icon seleccionado = b.getSelectedIcon();
            if (seleccionado != null) {
                icono = seleccionado;
            }
        }
        if (!modelo.isEnabled()) {
            if (modelo.isSelected()) {
                delEstado = b.getDisabledSelectedIcon();
                if (delEstado == null) {
                    delEstado = b.getSelectedIcon();
                }
            }
            if (delEstado == null) {
                delEstado = b.getDisabledIcon();
            }
        } else if (modelo.isPressed() && modelo.isArmed()) {
            delEstado = b.getPressedIcon();
            if (delEstado != null) {
                clearTextShiftOffset();
            }
        } else if (b.isRolloverEnabled() && modelo.isRollover()) {
            if (modelo.isSelected()) {
                delEstado = b.getRolloverSelectedIcon();
                if (delEstado == null) {
                    delEstado = b.getSelectedIcon();
                }
            }
            if (delEstado == null) {
                delEstado = b.getRolloverIcon();
            }
        }
        if (delEstado != null) {
            icono = delEstado;
        }
        if (modelo.isPressed() && modelo.isArmed()) {
            icono.paintIcon(c, g, iconRect.x + getTextShiftOffset(),
                    iconRect.y + getTextShiftOffset());
        } else {
            icono.paintIcon(c, g, iconRect.x, iconRect.y);
        }
    }

    protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
        paintText(g, (AbstractButton) c, textRect, text);
    }

    /**
     * Pinta el texto: en el color del frente si esta habilitado, en relieve si no.
     *
     * <p>El relieve es el del aspecto basico: el fondo aclarado en su lugar y el fondo oscurecido
     * un pixel arriba y a la izquierda. Metal lo reemplaza por un gris plano; ver la nota de la
     * clase.
     */
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        ButtonModel modelo = b.getModel();
        FontMetrics fm = b.getFontMetrics(b.getFont());
        int indice = b.getDisplayedMnemonicIndex();
        if (modelo.isEnabled()) {
            g.setColor(b.getForeground());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, indice,
                    textRect.x + getTextShiftOffset(),
                    textRect.y + fm.getAscent() + getTextShiftOffset());
        } else {
            g.setColor(b.getBackground().brighter());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, indice, textRect.x,
                    textRect.y + fm.getAscent());
            g.setColor(b.getBackground().darker());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, indice, textRect.x - 1,
                    textRect.y + fm.getAscent() - 1);
        }
    }

    /** Nada: el aspecto basico no marca el foco; los que derivan de el, si. */
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
            Rectangle textRect, Rectangle iconRect) {
    }

    /** Nada: el aspecto basico muestra lo apretado solo con el borde. */
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
    }

    protected void clearTextShiftOffset() {
        corrimiento = 0;
    }

    protected void setTextShiftOffset() {
        corrimiento = defaultTextShiftOffset;
    }

    protected int getTextShiftOffset() {
        return corrimiento;
    }

    /** El minimo es el preferido: un boton no se achica sin recortar el texto. */
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** Lo que ocupan icono y texto en una vista infinita, mas los insets. */
    public Dimension getPreferredSize(JComponent c) {
        AbstractButton b = (AbstractButton) c;
        return BasicGraphicsUtils.getPreferredButtonSize(b, b.getIconTextGap());
    }

    /** El maximo es el preferido: un boton no crece por si solo. */
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** La linea de base del texto, ubicado en esa caja; {@code -1} sin texto. */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        AbstractButton b = (AbstractButton) c;
        String texto = b.getText();
        if (texto == null || texto.isEmpty()) {
            return -1;
        }
        FontMetrics fm = b.getFontMetrics(b.getFont());
        Rectangle vistaR = new Rectangle();
        Rectangle iconoR = new Rectangle();
        Rectangle textoR = new Rectangle();
        ubicar(b, fm, width, height, vistaR, iconoR, textoR);
        return textoR.y + fm.getAscent();
    }

    /** Como se mueve la linea de base: segun donde este alineado el texto verticalmente. */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        int v = ((AbstractButton) c).getVerticalAlignment();
        if (v == AbstractButton.TOP) {
            return Component$BaselineResizeBehavior.CONSTANT_ASCENT;
        }
        if (v == AbstractButton.BOTTOM) {
            return Component$BaselineResizeBehavior.CONSTANT_DESCENT;
        }
        if (v == AbstractButton.CENTER) {
            return Component$BaselineResizeBehavior.CENTER_OFFSET;
        }
        return Component$BaselineResizeBehavior.OTHER;
    }
}
