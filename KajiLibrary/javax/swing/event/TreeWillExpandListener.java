package javax.swing.event;

import java.util.EventListener;

import javax.swing.tree.ExpandVetoException;

/**
 * Whoever can <strong>object</strong> to a branch being opened or closed.
 *
 * <h2>The difference from {@link TreeExpansionListener}</h2>
 *
 * <p>That one reports when it has already happened; this one asks beforehand. And the question is
 * real: by throwing an {@link ExpandVetoException} the listener cancels the operation, and the
 * tree stays as it was.
 *
 * <p>That the exception is checked is what forces the tree to foresee it instead of assuming that
 * the expansion always happens. It serves, for instance, for a branch that loads its children
 * from the network and wants to refuse if there is no connection -- better not to open it than to
 * open it empty.
 */
public interface TreeWillExpandListener extends EventListener {

    /** The branch is about to open. */
    void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException;

    /** The branch is about to close. */
    void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException;
}
