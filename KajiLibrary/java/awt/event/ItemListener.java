package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que se eligió o se dejó de elegir un elemento.
 */
public interface ItemListener extends EventListener {

    /** Cambió qué está elegido. */
    void itemStateChanged(ItemEvent e);
}
