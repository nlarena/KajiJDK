package javax.swing.plaf.basic;

import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.plaf.ComponentUI;

/**
 * The basic look and feel of a check box menu item.
 *
 * <p>Two things of its own. The first is the tick: {@link BasicMenuItemUI} leaves
 * {@link BasicMenuItemUI#checkIcon} null, and here the one from
 * {@link BasicIconFactory#getCheckBoxMenuItemIcon} is put in, which draws only when the item is
 * ticked.
 *
 * <p>The second is {@link #processMouseEvent}: releasing the button over a tickable item fires
 * it <em>and</em> closes the menu, just like an ordinary item. That the item has state does not
 * change that; what changes is that after closing it is left with the tick on.
 *
 * <h2>The tick is not the same size as Metal's</h2>
 *
 * <p>The basic one measures 9 x 9 and Metal's 10 x 10, and that is why a tickable item's
 * preferred width does not match the JDK's: it is two pixels. It is the same gap as everywhere
 * in the package -- with no look and feel table installed, the icons are the basic ones and not
 * the real look and feel's --, and it is said here so that it does not look like an arithmetic
 * mistake.
 */
public class BasicCheckBoxMenuItemUI extends BasicMenuItemUI {

    public BasicCheckBoxMenuItemUI() {
    }

    /** A new one per item: it keeps the component and its listeners. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicCheckBoxMenuItemUI();
    }

    protected String getPropertyPrefix() {
        return "CheckBoxMenuItem";
    }

    /** The usual plus the tick; see the class note. */
    protected void installDefaults() {
        super.installDefaults();
        checkIcon = BasicIconFactory.getCheckBoxMenuItemIcon();
    }

    /**
     * It fires the item if the mouse was released over it, and closes the menu.
     *
     * <p>{@code MenuSelectionManager} calls it while the menu is open: at that moment the item does
     * not receive events of its own, because the one that hands them out is the manager.
     */
    public void processMouseEvent(JMenuItem item, MouseEvent e, MenuElement[] path,
            MenuSelectionManager manager) {
        java.awt.Point p = e.getPoint();
        if (p.x >= 0 && p.x < item.getWidth() && p.y >= 0 && p.y < item.getHeight()) {
            if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                manager.clearSelectedPath();
                item.doClick(0);
                item.setArmed(false);
            } else {
                manager.setSelectedPath(path);
            }
        } else if (item.getModel().isArmed()) {
            int c = path.length - 1;
            MenuElement[] newPath = new MenuElement[c];
            for (int i = 0; i < c; i++) {
                newPath[i] = path[i];
            }
            manager.setSelectedPath(newPath);
        }
    }
}
