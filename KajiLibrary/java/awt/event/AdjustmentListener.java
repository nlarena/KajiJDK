package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que se movió una barra de desplazamiento.
 */
public interface AdjustmentListener extends EventListener {

    /** Cambió el valor. */
    void adjustmentValueChanged(AdjustmentEvent e);
}
