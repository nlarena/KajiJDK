package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que la seleccion de una lista cambio.
 */
public interface ListSelectionListener extends EventListener {

    /** La seleccion cambio. */
    void valueChanged(ListSelectionEvent e);
}
