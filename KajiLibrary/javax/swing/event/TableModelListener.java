package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que los datos de una tabla cambiaron.
 */
public interface TableModelListener extends EventListener {

    /** El modelo cambio; el evento dice que parte. */
    void tableChanged(TableModelEvent e);
}
