package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que la seleccion del arbol cambio.
 */
public interface TreeSelectionListener extends EventListener {

    /** La seleccion cambio. */
    void valueChanged(TreeSelectionEvent e);
}
