package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear about the mouse dragged over a menu; see
 * {@link MenuDragMouseEvent}.
 */
public interface MenuDragMouseListener extends EventListener {

    /** The drag entered the element. */
    void menuDragMouseEntered(MenuDragMouseEvent e);

    /** The drag left the element. */
    void menuDragMouseExited(MenuDragMouseEvent e);

    /** The drag moved. */
    void menuDragMouseDragged(MenuDragMouseEvent e);

    /** The button was released. */
    void menuDragMouseReleased(MenuDragMouseEvent e);
}
