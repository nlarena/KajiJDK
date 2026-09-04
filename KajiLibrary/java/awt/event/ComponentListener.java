package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que un componente cambió de tamaño, de lugar o de visibilidad.

 <p>Estos eventos llegan **después** del cambio, así que sirven para reaccionar y no para vetarlo.
 */
public interface ComponentListener extends EventListener {

    /** Cambió de tamaño. */
    void componentResized(ComponentEvent e);

    /** Cambió de lugar. */
    void componentMoved(ComponentEvent e);

    /** Se hizo visible. */
    void componentShown(ComponentEvent e);

    /** Se ocultó. */
    void componentHidden(ComponentEvent e);
}
