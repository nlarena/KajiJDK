package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that the mouse wheel was moved.
 */
public interface MouseWheelListener extends EventListener {

    /** The wheel was moved. */
    void mouseWheelMoved(MouseWheelEvent e);
}
