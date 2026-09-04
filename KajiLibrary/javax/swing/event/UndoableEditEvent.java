package javax.swing.event;

import java.util.EventObject;

import javax.swing.undo.UndoableEdit;

/**
 * El aviso de que ocurrio algo deshacible.
 *
 * <p>Lleva la edicion misma, no una descripcion: quien lo recibe puede guardarla y despues pedirle
 * que se deshaga. Es la diferencia entre notificar y delegar — el evento no cuenta que paso, entrega
 * el objeto que sabe revertirlo.
 */
public class UndoableEditEvent extends EventObject {

    private static final long serialVersionUID = 4418044561803547969L;

    private UndoableEdit myEdit;

    /**
     * @param source quien produjo la edicion
     * @param edit la edicion, que sabe deshacerse y rehacerse
     */
    public UndoableEditEvent(Object source, UndoableEdit edit) {
        super(source);
        this.myEdit = edit;
    }

    /** La edicion que ocurrio. */
    public UndoableEdit getEdit() {
        return this.myEdit;
    }
}
