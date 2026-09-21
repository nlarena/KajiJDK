package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a menu opened or closed.
 */
public interface MenuListener extends EventListener {

    /** The menu was selected. */
    void menuSelected(MenuEvent e);

    /** The menu was deselected. */
    void menuDeselected(MenuEvent e);

    /** The menu was cancelled. */
    void menuCanceled(MenuEvent e);
}
