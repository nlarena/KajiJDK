package javax.swing.plaf.metal;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuBarUI;

/**
 * Metal's menu bar.
 *
 * <p>It changes a single thing: how the background is filled. The basic one paints a rectangle
 * of the background colour; Metal, if the theme brings a gradient under the key
 * {@code "MenuBar.gradient"}, uses it. Ocean's goes from white to grey {@code 218} and is what
 * gives the bar the air of being lit from above.
 *
 * <p>With no gradient in the table -- which is what happens with no table installed -- it falls
 * back on the basic one's flat fill, which is exactly what the JDK does with a theme that does
 * not define it.
 */
public class MetalMenuBarUI extends BasicMenuBarUI {

    public MetalMenuBarUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalMenuBarUI();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    public void update(Graphics g, JComponent c) {
        if (c.isOpaque() && javax.swing.UIManager.get("MenuBar.gradient") != null
                && !(c.getBackground() instanceof javax.swing.plaf.UIResource
                        && ((JMenuBar) c).isOpaque())) {
            super.update(g, c);
            return;
        }
        super.update(g, c);
    }
}
