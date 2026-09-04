package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que el contenido de una lista cambio.
 */
public interface ListDataListener extends EventListener {

    /** Se agregaron elementos. */
    void intervalAdded(ListDataEvent e);

    /** Se sacaron elementos. */
    void intervalRemoved(ListDataEvent e);

    /** Cambiaron elementos, sin cambiar cuantos hay. */
    void contentsChanged(ListDataEvent e);
}
