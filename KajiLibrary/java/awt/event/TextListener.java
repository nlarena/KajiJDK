package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que cambió el texto de un componente.
 */
public interface TextListener extends EventListener {

    /** Cambió el texto. */
    void textValueChanged(TextEvent e);
}
