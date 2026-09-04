package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que el arbol cambio.
 */
public interface TreeModelListener extends EventListener {

    /** Cambiaron nodos, sin cambiar la estructura. */
    void treeNodesChanged(TreeModelEvent e);

    /** Se insertaron nodos. */
    void treeNodesInserted(TreeModelEvent e);

    /** Se sacaron nodos. */
    void treeNodesRemoved(TreeModelEvent e);

    /** La estructura cambio por completo. */
    void treeStructureChanged(TreeModelEvent e);
}
