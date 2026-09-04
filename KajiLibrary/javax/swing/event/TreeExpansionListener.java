package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que una rama se abrio o se cerro.
 */
public interface TreeExpansionListener extends EventListener {

    /** La rama se abrio. */
    void treeExpanded(TreeExpansionEvent e);

    /** La rama se cerro. */
    void treeCollapsed(TreeExpansionEvent e);
}
