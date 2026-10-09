package javax.swing.event;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

/**
 * The mouse was dragged over an open menu.
 *
 * <p>It is a {@link MouseEvent} <strong>plus the path</strong>. Without the path, a menu item
 * that receives a drag would not know which submenu the mouse is coming from, and that is what
 * decides what to close when going from one branch to another. See {@link MenuElement}.
 */
public class MenuDragMouseEvent extends MouseEvent {

    private static final long serialVersionUID = 1L;

    private MenuElement[] path;
    private MenuSelectionManager manager;

    /** Without click count or button, which in a drag add nothing. */
    public MenuDragMouseEvent(Component source, int id, long when, int modifiers, int x, int y,
            int clickCount, boolean popupTrigger, MenuElement[] p, MenuSelectionManager m) {
        super(source, id, when, modifiers, x, y, clickCount, popupTrigger);
        this.path = p;
        this.manager = m;
    }

    /** With absolute position and button. */
    public MenuDragMouseEvent(Component source, int id, long when, int modifiers, int x, int y,
            int xAbs, int yAbs, int clickCount, boolean popupTrigger, MenuElement[] p,
            MenuSelectionManager m) {
        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger,
                MouseEvent.NOBUTTON);
        this.path = p;
        this.manager = m;
    }

    /** The path from the bar to the element. */
    public MenuElement[] getPath() {
        return this.path;
    }

    /** Who manages the menu's selection. */
    public MenuSelectionManager getMenuSelectionManager() {
        return this.manager;
    }
}
