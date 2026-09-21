package javax.swing.undo;

import java.util.Hashtable;

/**
 * An object that knows how to save and restore its own state in a table.
 *
 * <h2>The other way of undoing</h2>
 *
 * <p>An ordinary {@link UndoableEdit} knows how to <em>revert an action</em>: it knows the
 * operation and its inverse. This is the opposite approach -- what is kept is not the operation
 * but <strong>a snapshot of the state before and another after</strong>, and undoing is putting
 * the first one back. See {@link StateEdit}, which is what takes them.
 *
 * <p>It serves when an operation's inverse is hard or impossible to write, and it costs memory:
 * two copies of the state for each step.
 *
 * <p>The two signatures are not symmetric and that is deliberate: saving takes a table that can
 * be written, restoring takes one that is only read.
 */
public interface StateEditable {

    /** The JDK's version identifier; it is kept for surface fidelity. */
    public static final String RCSID = "$Id: StateEditable.java,v 1.2 1997/09/08 19:39:08 marklin Exp $";

    /** Saves in {@code state} whatever is needed to come back to this state. */
    void storeState(Hashtable<Object, Object> state);

    /** Goes back to the state {@code state} describes. */
    void restoreState(Hashtable<?, ?> state);
}
