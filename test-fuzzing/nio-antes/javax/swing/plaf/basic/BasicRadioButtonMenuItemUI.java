package javax.swing.plaf.basic;

import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.plaf.ComponentUI;

/**
 * The basic look and feel of a radio button menu item.
 *
 * <p>The same as {@link BasicCheckBoxMenuItemUI} save for the icon: a 6 x 6 dot instead of a
 * 9 x 9 tick. The difference in drawing is the whole difference -- a tick says "this is on" and
 * a dot says "of this group, this one" --, and who enforces the "of this group" is not the look
 * and feel but the {@code ButtonGroup} that groups the items.
 *
 * <p>The same note as in the tickable one holds: the basic dot measures 6 x 6 and Metal's
 * 10 x 10, so the preferred width does not match the JDK's.
 */
public class BasicRadioButtonMenuItemUI extends BasicMenuItemUI {

    public BasicRadioButtonMenuItemUI() {
    }

    /** A new one per item: it keeps the component and its listeners. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicRadioButtonMenuItemUI();
    }

    protected String getPropertyPrefix() {
        return "RadioButtonMenuItem";
    }

    /** The usual plus the dot; see the class note. */
    protected void installDefaults() {
        super.installDefaults();
        checkIcon = BasicIconFactory.getRadioButtonMenuItemIcon();
    }

    /** The same as in the tickable one: releasing over it fires and closes. */
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
