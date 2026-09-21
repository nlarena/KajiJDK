package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that the tree changed.
 */
public interface TreeModelListener extends EventListener {

    /** Nodes changed, without the structure changing. */
    void treeNodesChanged(TreeModelEvent e);

    /** Nodes were inserted. */
    void treeNodesInserted(TreeModelEvent e);

    /** Nodes were removed. */
    void treeNodesRemoved(TreeModelEvent e);

    /** The structure changed completely. */
    void treeStructureChanged(TreeModelEvent e);
}
