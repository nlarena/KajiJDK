package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.ViewportUI;

/**
 * The basic look and feel of a scroll viewport: it gives it colours and a typeface, and
 * nothing else.
 *
 * <p>It does not paint: {@code ComponentUI.update} fills the background if the viewport is
 * opaque -- it is, from its constructor -- and the content is painted by the view. The values
 * are those of {@code Viewport.*} measured in Metal (JDK 25): background (238, 238, 238),
 * foreground (51, 51, 51) and Dialog 12.
 */
public class BasicViewportUI extends ViewportUI {

    private static ViewportUI viewportUI = new BasicViewportUI();

    private static final ColorUIResource DEFAULT_BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource DEFAULT_FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final Font DEFAULT_FONT = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicViewportUI() {
    }

    /** The shared look and feel: it keeps nothing of the viewport. */
    public static ComponentUI createUI(JComponent c) {
        return viewportUI;
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        installDefaults(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        uninstallDefaults(c);
    }

    /** Colours and typeface, only where the user did not set their own; see {@link UIResource}. */
    protected void installDefaults(JComponent c) {
        Color background = c.getBackground();
        if (background == null || background instanceof UIResource) {
            c.setBackground(DEFAULT_BACKGROUND);
        }
        Color foreground = c.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            c.setForeground(DEFAULT_FOREGROUND);
        }
        Font font = c.getFont();
        if (font == null || font instanceof UIResource) {
            c.setFont(DEFAULT_FONT);
        }
    }

    /** What was installed stays in the component; the JDK does not erase it either. */
    protected void uninstallDefaults(JComponent c) {
    }
}
