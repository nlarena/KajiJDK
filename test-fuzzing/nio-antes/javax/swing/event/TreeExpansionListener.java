package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a branch was opened or closed.
 */
public interface TreeExpansionListener extends EventListener {

    /** The branch was opened. */
    void treeExpanded(TreeExpansionEvent e);

    /** The branch was closed. */
    void treeCollapsed(TreeExpansionEvent e);
}
