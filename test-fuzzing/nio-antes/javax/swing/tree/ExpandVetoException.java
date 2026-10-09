package javax.swing.tree;

import javax.swing.event.TreeExpansionEvent;

/**
 * Somebody objected to a branch being opened or closed.
 *
 * <p>It is a <em>veto</em>, not an error: the mechanism is that the tree asks before expanding
 * and any listener can refuse by throwing this. That it is a checked exception is what forces the
 * tree to foresee it instead of assuming that the expansion always happens.
 *
 * <p>It carries inside the event that was about to be processed, so that whoever catches it knows
 * which branch it was about.
 */
public class ExpandVetoException extends Exception {

    private static final long serialVersionUID = 1L;

    /** The event that was vetoed. */
    protected TreeExpansionEvent event;

    /** Without a message. */
    public ExpandVetoException(TreeExpansionEvent event) {
        this(event, null);
    }

    /** With a message explaining the reason for the veto. */
    public ExpandVetoException(TreeExpansionEvent event, String message) {
        super(message);
        this.event = event;
    }
}
