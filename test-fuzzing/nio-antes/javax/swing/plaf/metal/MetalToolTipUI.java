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
 * Metal's tooltip, which also shows the shortcut.
 *
 * <p>A Metal tooltip does not say only the text: if the component that asks for it has a
 * mnemonic key or an accelerator, it adds it on the right separated by
 * {@value #padSpaceBetweenStrings} pixels. It is the only way a Swing program has of teaching
 * its shortcuts without writing them by hand in every tooltip text.
 *
 * <p>The accelerator is looked for in two places and in this order: first the registered
 * {@code KeyStroke}, and if there is none, the button's mnemonic letter. A button with
 * {@code setMnemonic('x')} shows {@code "Alt-X"}.
 *
 * <h2>What is said</h2>
 *
 * <p>{@link #isAcceleratorHidden} reads {@code "ToolTip.hideAccelerator"} from the look and
 * feel's table. With no table it answers no, which is the same as the JDK answers with Metal's
 * table.
 */
public class MetalToolTipUI extends BasicToolTipUI {

    /** The pixels between the text and the shortcut. */
    public static final int padSpaceBetweenStrings = 12;

    private static final MetalToolTipUI SHARED = new MetalToolTipUI();

    private JToolTip tip;
    private Font smallFont;

    public MetalToolTipUI() {
    }

    /** It shares its instance, like the label. */
    public static ComponentUI createUI(JComponent c) {
        return SHARED;
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        tip = (JToolTip) c;
        Font f = c.getFont();
        smallFont = (f == null) ? null : new Font(f.getName(), Font.PLAIN, f.getSize() - 2);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        tip = null;
        smallFont = null;
    }

    /** No; see the class note. */
    protected boolean isAcceleratorHidden() {
        Object o = javax.swing.UIManager.get("ToolTip.hideAccelerator");
        return Boolean.TRUE.equals(o);
    }

    /**
     * The shortcut of the component that asked for the tooltip, ready to draw.
     *
     * @return the text, or empty if there is no shortcut or it is hidden
     */
    public String getAcceleratorString() {
        if (tip == null || isAcceleratorHidden()) {
            return "";
        }
        JComponent comp = tip.getComponent();
        if (comp == null) {
            return "";
        }
        KeyStroke[] keyStrokes = comp.getRegisteredKeyStrokes();
        int condition = comp.getConditionForKeyStroke(null);
        for (int i = 0; keyStrokes != null && i < keyStrokes.length; i++) {
            if (comp.getConditionForKeyStroke(keyStrokes[i])
                    == JComponent.WHEN_IN_FOCUSED_WINDOW) {
                return text(keyStrokes[i]);
            }
        }
        if (comp instanceof AbstractButton) {
            int mnem = ((AbstractButton) comp).getMnemonic();
            if (mnem != 0) {
                return text(KeyStroke.getKeyStroke(mnem, java.awt.event.InputEvent.ALT_MASK));
            }
        }
        if (condition == JComponent.UNDEFINED_CONDITION) {
            return "";
        }
        return "";
    }

    /** {@code "Alt-X"}, with a hyphen and not with a plus, which is how Metal writes it. */
    private static String text(KeyStroke k) {
        if (k == null) {
            return "";
        }
        String mods = java.awt.event.KeyEvent.getKeyModifiersText(k.getModifiers());
        String keyStroke = java.awt.event.KeyEvent.getKeyText(k.getKeyCode());
        if (mods == null || mods.length() == 0) {
            return keyStroke;
        }
        return mods + "-" + keyStroke;
    }

    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
        String accelerator = getAcceleratorString();
        if (accelerator.length() == 0) {
            return;
        }
        Font before = g.getFont();
        if (smallFont != null) {
            g.setFont(smallFont);
        }
        FontMetrics fm = c.getFontMetrics(g.getFont());
        Insets i = c.getInsets();
        Dimension s = c.getSize();
        g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
        g.drawString(accelerator, s.width - i.right - fm.stringWidth(accelerator) - 3,
                i.top + fm.getAscent() + 3);
        g.setFont(before);
    }

    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        String accelerator = getAcceleratorString();
        if (accelerator.length() > 0 && smallFont != null) {
            d.width += c.getFontMetrics(smallFont).stringWidth(accelerator)
                    + padSpaceBetweenStrings;
        }
        return d;
    }
}
