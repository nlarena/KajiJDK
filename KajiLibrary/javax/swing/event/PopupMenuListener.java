package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a popup menu appears or goes away.
 */
public interface PopupMenuListener extends EventListener {

    /** It is about to be shown. */
    void popupMenuWillBecomeVisible(PopupMenuEvent e);

    /** It is about to be hidden. */
    void popupMenuWillBecomeInvisible(PopupMenuEvent e);

    /** It was cancelled. */
    void popupMenuCanceled(PopupMenuEvent e);
}
