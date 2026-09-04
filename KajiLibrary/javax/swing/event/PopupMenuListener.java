package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que un menu emergente aparece o se va.
 */
public interface PopupMenuListener extends EventListener {

    /** Esta por mostrarse. */
    void popupMenuWillBecomeVisible(PopupMenuEvent e);

    /** Esta por esconderse. */
    void popupMenuWillBecomeInvisible(PopupMenuEvent e);

    /** Se cancelo. */
    void popupMenuCanceled(PopupMenuEvent e);
}
