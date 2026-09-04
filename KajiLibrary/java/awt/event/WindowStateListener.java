package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que una ventana se minimizó, se maximizó o se restauró.
 */
public interface WindowStateListener extends EventListener {

    /** Cambió el estado de la ventana. */
    void windowStateChanged(WindowEvent e);
}
