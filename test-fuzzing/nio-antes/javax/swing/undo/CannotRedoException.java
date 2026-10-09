package javax.swing.undo;

/**
 * A redo was asked for on something that cannot be redone.
 *
 * <p>{@link CannotUndoException}'s twin; see there for the argument on why it carries no
 * message.
 */
public class CannotRedoException extends RuntimeException {

    private static final long serialVersionUID = 1097001200L;

    public CannotRedoException() {
        super();
    }
}
