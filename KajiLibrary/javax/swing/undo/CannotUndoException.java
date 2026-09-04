package javax.swing.undo;

/**
 * Se pidio deshacer algo que no se puede deshacer.
 *
 * <p>No tiene mensaje ni constructor con detalle, y asi es en el JDK: la unica informacion util es
 * cual era la edicion, y esa la tiene quien llamo. Que sea no chequeada es coherente con eso — la
 * pregunta {@code canUndo} existe justamente para no llegar aca.
 */
public class CannotUndoException extends RuntimeException {

    private static final long serialVersionUID = 1097001100L;

    public CannotUndoException() {
        super();
    }
}
