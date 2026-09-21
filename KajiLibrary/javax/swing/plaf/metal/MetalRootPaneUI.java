package javax.swing.plaf.metal;

import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRootPaneUI;

/**
 * Metal's root pane, which is the one that knows how to decorate a window.
 *
 * <p>Metal is the only Java look and feel that can draw a window's frame and title bar
 * <em>on Java's side</em>, instead of leaving them to the system. That is what
 * {@code JFrame.setDefaultLookAndFeelDecorated(true)} does, and what implements it is this
 * class: it looks at {@link JRootPane#getWindowDecorationStyle} and, when it is not
 * {@code NONE}, puts a title bar of its own as a child of the root pane and changes its border.
 *
 * <p>The style may change on the fly, and that is why {@link #propertyChange} listens to
 * {@code "windowDecorationStyle"}: a dialog that goes from normal to error has to change its
 * frame without being created again.
 *
 * <h2>What is said and not covered up</h2>
 *
 * <p>The decoration is not assembled. It needs a real window -- to take the border away from the
 * system, to drag it and to resize it -- and this VM has no windows. What is there is everything
 * that can be answered without one: the UI installs itself, listens to the style change and
 * uninstalls itself.
 */
public class MetalRootPaneUI extends BasicRootPaneUI {

    public MetalRootPaneUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalRootPaneUI();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);
    }
}
