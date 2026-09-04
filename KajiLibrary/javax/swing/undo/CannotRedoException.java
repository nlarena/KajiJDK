package javax.swing.undo;

/**
 * Se pidio rehacer algo que no se puede rehacer.
 *
 * <p>El gemelo de {@link CannotUndoException}; ver alli el argumento sobre por que no lleva mensaje.
 */
public class CannotRedoException extends RuntimeException {

    private static final long serialVersionUID = 1097001200L;

    public CannotRedoException() {
        super();
    }
}
