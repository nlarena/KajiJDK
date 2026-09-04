package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que una ventana ganó o perdió el foco.
 */
public interface WindowFocusListener extends EventListener {

    /** La ventana ganó el foco. */
    void windowGainedFocus(WindowEvent e);

    /** La ventana perdió el foco. */
    void windowLostFocus(WindowEvent e);
}
