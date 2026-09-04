package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que un componente ganó o perdió el foco del teclado.
 */
public interface FocusListener extends EventListener {

    /** Ganó el foco. */
    void focusGained(FocusEvent e);

    /** Perdió el foco. */
    void focusLost(FocusEvent e);
}
