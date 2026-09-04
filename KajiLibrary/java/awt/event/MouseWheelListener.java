package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que se movió la rueda del ratón.
 */
public interface MouseWheelListener extends EventListener {

    /** Se movió la rueda. */
    void mouseWheelMoved(MouseWheelEvent e);
}
