package javax.swing.event;

import java.util.EventObject;

import javax.swing.tree.TreePath;

/**
 * A branch of the tree was opened or closed.
 *
 * <p>It carries the path and not the node, for the usual reason in a tree: one same object may
 * hang from two places, and only the path says which one was opened. See {@link TreePath}.
 *
 * <p>Which of the two things happened is said by the {@link TreeExpansionListener} method it
 * reaches.
 */
public class TreeExpansionEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** The branch's path. */
    protected TreePath path;

    public TreeExpansionEvent(Object source, TreePath path) {
        super(source);
        this.path = path;
    }

    /** The path of the branch that was opened or closed. */
    public TreePath getPath() {
        return this.path;
    }
}
