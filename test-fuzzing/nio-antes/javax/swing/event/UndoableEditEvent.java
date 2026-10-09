package javax.swing.event;

import java.util.EventObject;

import javax.swing.undo.UndoableEdit;

/**
 * The notice that something undoable happened.
 *
 * <p>It carries the edit itself, not a description: whoever receives it can keep it and later ask
 * it to undo itself. It is the difference between notifying and delegating -- the event does not
 * tell what happened, it hands over the object that knows how to revert it.
 */
public class UndoableEditEvent extends EventObject {

    private static final long serialVersionUID = 4418044561803547969L;

    private UndoableEdit myEdit;

    /**
     * @param source who produced the edit
     * @param edit the edit, which knows how to undo and redo itself
     */
    public UndoableEditEvent(Object source, UndoableEdit edit) {
        super(source);
        this.myEdit = edit;
    }

    /** The edit that happened. */
    public UndoableEdit getEdit() {
        return this.myEdit;
    }
}
