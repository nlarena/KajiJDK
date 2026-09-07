package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

/**
 * El globo de ayuda de Metal, que ademas muestra el atajo.
 *
 * <p>Un globo de Metal no dice solo el texto: si el componente que lo pide tiene una tecla
 * mnemonica o un acelerador, lo agrega a la derecha separado por {@value #padSpaceBetweenStrings}
 * pixeles. Es la unica forma que tiene un programa de Swing de ensenar sus atajos sin escribirlos
 * a mano en cada texto de ayuda.
 *
 * <p>El acelerador se busca en dos lados y en este orden: primero el {@code KeyStroke} registrado,
 * y si no hay, la letra mnemonica del boton. Un boton con {@code setMnemonic('x')} muestra
 * {@code "Alt-X"}.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>{@link #isAcceleratorHidden} lee {@code "ToolTip.hideAccelerator"} de la tabla del aspecto.
 * Sin tabla contesta que no, que es lo mismo que contesta el JDK con la tabla de Metal.
 */
public class MetalToolTipUI extends BasicToolTipUI {

    /** Los pixeles entre el texto y el atajo. */
    public static final int padSpaceBetweenStrings = 12;

    private static final MetalToolTipUI UNICO = new MetalToolTipUI();

    private JToolTip tip;
    private Font fuenteChica;

    public MetalToolTipUI() {
    }

    /** Comparte instancia, como la etiqueta. */
    public static ComponentUI createUI(JComponent c) {
        return UNICO;
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        tip = (JToolTip) c;
        Font f = c.getFont();
        fuenteChica = (f == null) ? null : new Font(f.getName(), Font.PLAIN, f.getSize() - 2);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        tip = null;
        fuenteChica = null;
    }

    /** No; ver la nota de la clase. */
    protected boolean isAcceleratorHidden() {
        Object o = javax.swing.UIManager.get("ToolTip.hideAccelerator");
        return Boolean.TRUE.equals(o);
    }

    /**
     * El atajo del componente que pidio el globo, listo para dibujar.
     *
     * @return el texto, o vacio si no hay atajo o esta escondido
     */
    public String getAcceleratorString() {
        if (tip == null || isAcceleratorHidden()) {
            return "";
        }
        JComponent comp = tip.getComponent();
        if (comp == null) {
            return "";
        }
        KeyStroke[] teclas = comp.getRegisteredKeyStrokes();
        int condicion = comp.getConditionForKeyStroke(null);
        for (int i = 0; teclas != null && i < teclas.length; i++) {
            if (comp.getConditionForKeyStroke(teclas[i])
                    == JComponent.WHEN_IN_FOCUSED_WINDOW) {
                return texto(teclas[i]);
            }
        }
        if (comp instanceof AbstractButton) {
            int mnem = ((AbstractButton) comp).getMnemonic();
            if (mnem != 0) {
                return texto(KeyStroke.getKeyStroke(mnem, java.awt.event.InputEvent.ALT_MASK));
            }
        }
        if (condicion == JComponent.UNDEFINED_CONDITION) {
            return "";
        }
        return "";
    }

    /** {@code "Alt-X"}, con guion y no con mas, que es como lo escribe Metal. */
    private static String texto(KeyStroke k) {
        if (k == null) {
            return "";
        }
        String mods = java.awt.event.KeyEvent.getKeyModifiersText(k.getModifiers());
        String tecla = java.awt.event.KeyEvent.getKeyText(k.getKeyCode());
        if (mods == null || mods.length() == 0) {
            return tecla;
        }
        return mods + "-" + tecla;
    }

    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
        String atajo = getAcceleratorString();
        if (atajo.length() == 0) {
            return;
        }
        Font antes = g.getFont();
        if (fuenteChica != null) {
            g.setFont(fuenteChica);
        }
        FontMetrics fm = c.getFontMetrics(g.getFont());
        Insets i = c.getInsets();
        Dimension s = c.getSize();
        g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
        g.drawString(atajo, s.width - i.right - fm.stringWidth(atajo) - 3,
                i.top + fm.getAscent() + 3);
        g.setFont(antes);
    }

    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        String atajo = getAcceleratorString();
        if (atajo.length() > 0 && fuenteChica != null) {
            d.width += c.getFontMetrics(fuenteChica).stringWidth(atajo)
                    + padSpaceBetweenStrings;
        }
        return d;
    }
}
