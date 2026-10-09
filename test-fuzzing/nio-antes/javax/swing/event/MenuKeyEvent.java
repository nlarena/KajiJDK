package javax.swing.event;

import java.awt.Component;
import java.awt.event.KeyEvent;

import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

/**
 * The keyboard was used with an open menu.
 *
 * <p>{@link MenuDragMouseEvent}'s twin for keys, and with the same reason for being: navigating a
 * menu with the arrows needs to know which branch one is in, and that is said by the path, not by
 * the element on its own.
 */
public class MenuKeyEvent extends KeyEvent {

    private static final long serialVersionUID = 1L;

    private MenuElement[] path;
    private MenuSelectionManager manager;

    public MenuKeyEvent(Component source, int id, long when, int modifiers, int keyCode,
            char keyChar, MenuElement[] p, MenuSelectionManager m) {
        super(source, id, when, modifiers, keyCode, keyChar);
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
