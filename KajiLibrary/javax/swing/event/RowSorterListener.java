package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que el orden de las filas cambio.
 */
public interface RowSorterListener extends EventListener {

    /** Cambio el orden o las claves de ordenamiento. */
    void sorterChanged(RowSorterEvent e);
}
