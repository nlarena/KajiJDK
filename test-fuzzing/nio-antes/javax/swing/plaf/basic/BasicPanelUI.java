package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.PanelUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a panel: colours, typeface, and nothing else.
 *
 * <p>A panel draws nothing of its own -- {@code ComponentUI.update} fills its background if it
 * is opaque and the children paint themselves --, so this look and feel has no {@code paint}.
 * The only thing it does is set the values that in the JDK come from {@code UIManager} under
 * {@code Panel.*}, measured in Metal (JDK 25): background (238, 238, 238), foreground
 * (51, 51, 51), Dialog 12, and the panel opaque.
 *
 * <p>A single object for every panel: {@link #createUI} always returns the same one, and it can
 * because it keeps nothing of any of them.
 *
 * <h2>Baseline</h2>
 *
 * <p>A panel has no text, so it has no baseline: {@link #getBaseline} returns -1 and the
 * behaviour on resizing is {@code OTHER}. It validates the arguments all the same -- a negative
 * size throws, a null component blows up --, which is what {@link ComponentUI} does.
 */
public class BasicPanelUI extends PanelUI {

    private static PanelUI panelUI = new BasicPanelUI();

    private static final ColorUIResource DEFAULT_BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource DEFAULT_FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final Font DEFAULT_FONT = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicPanelUI() {
    }

    /** The shared look and feel; see the class note. */
    public static ComponentUI createUI(JComponent c) {
        return panelUI;
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        installDefaults((JPanel) c);
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults((JPanel) c);
        super.uninstallUI(c);
    }

    /**
     * Colours, typeface and opacity, only where the user did not set their own; see
     * {@link UIResource}.
     */
    protected void installDefaults(JPanel p) {
        Color background = p.getBackground();
        if (background == null || background instanceof UIResource) {
            p.setBackground(DEFAULT_BACKGROUND);
        }
        Color foreground = p.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            p.setForeground(DEFAULT_FOREGROUND);
        }
        Font font = p.getFont();
        if (font == null || font instanceof UIResource) {
            p.setFont(DEFAULT_FONT);
        }
        LookAndFeel.installProperty(p, "opaque", Boolean.TRUE);
    }

    /**
     * It removes nothing: what was installed is a {@link UIResource} and the next look and feel
     * overwrites it.
     */
    protected void uninstallDefaults(JPanel p) {
    }

    /**
     * -1: a panel has no text and therefore has no baseline.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        return -1;
    }

    /**
     * {@code OTHER}: with no baseline there is nothing that moves with it.
     *
     * @throws NullPointerException if the component is null
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.OTHER;
    }
}
