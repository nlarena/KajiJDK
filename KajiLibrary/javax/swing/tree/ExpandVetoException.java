package javax.swing.tree;

import javax.swing.event.TreeExpansionEvent;

/**
 * Alguien se opuso a que una rama se abriera o cerrara.
 *
 * <p>Es un <em>veto</em>, no un error: el mecanismo es que el arbol pregunta antes de expandir y
 * cualquier oyente puede negarse tirando esto. Que sea una excepcion chequeada es lo que obliga al
 * arbol a preverlo en vez de asumir que la expansion siempre ocurre.
 *
 * <p>Lleva adentro el evento que se estaba por procesar, para que quien la atrape sepa de que rama
 * se trataba.
 */
public class ExpandVetoException extends Exception {

    private static final long serialVersionUID = 1L;

    /** El evento que se veto. */
    protected TreeExpansionEvent event;

    /** Sin mensaje. */
    public ExpandVetoException(TreeExpansionEvent event) {
        this(event, null);
    }

    /** Con un mensaje que explique el motivo del veto. */
    public ExpandVetoException(TreeExpansionEvent event, String message) {
        super(message);
        this.event = event;
    }
}
