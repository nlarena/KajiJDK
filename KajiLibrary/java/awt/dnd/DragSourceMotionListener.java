package java.awt.dnd;

import java.util.EventListener;

/**
 * Whoever wants to follow the movement of the mouse throughout the drag.
 *
 * <p>It is kept apart from {@link DragSourceListener} for the same reason that the movement of the
 * mouse is kept apart from its buttons: they are very many more events, and whoever only wants to
 * know where it was dropped should not pay for them.
 */
public interface DragSourceMotionListener extends EventListener {

    /** The mouse moved while dragging. */
    void dragMouseMoved(DragSourceDragEvent dsde);
}
