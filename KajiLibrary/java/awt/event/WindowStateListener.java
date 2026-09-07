package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a window was minimized, maximized or restored.
 */
public interface WindowStateListener extends EventListener {

    /** The window's state changed. */
    void windowStateChanged(WindowEvent e);
}
