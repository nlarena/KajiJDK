package javax.swing.undo;

/**
 * An undo was asked for on something that cannot be undone.
 *
 * <p>It has no message and no constructor with detail, and so it is in the JDK: the only useful
 * information is which edit it was, and the caller has that. That it is unchecked is consistent
 * with it -- the {@code canUndo} question exists precisely so as not to get here.
 */
public class CannotUndoException extends RuntimeException {

    private static final long serialVersionUID = 1097001100L;

    public CannotUndoException() {
        super();
    }
}
