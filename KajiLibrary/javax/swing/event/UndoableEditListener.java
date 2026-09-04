package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que ocurrio algo deshacible.
 *
 * <p>Es el enganche entre quien <em>produce</em> las ediciones —un documento, un modelo— y quien
 * las <em>administra</em>, tipicamente un {@link javax.swing.undo.UndoManager}. Esa separacion es
 * lo que permite que un documento no sepa nada de pilas de deshacer: solo avisa, y quien lleva la
 * cuenta se suscribe.
 */
public interface UndoableEditListener extends EventListener {

    /** Aviso de que se hizo algo que se puede deshacer. */
    void undoableEditHappened(UndoableEditEvent e);
}
