package javax.swing;

import java.util.EventObject;

import javax.swing.event.CellEditorListener;

/**
 * Who edits a cell of a table, a tree or a list.
 *
 * <h2>Why editing is a separate object</h2>
 *
 * <p>Editing a cell has a life cycle that does not fit in a component: it begins with a gesture
 * -- one click, two, a key --, goes on while the user types, and ends in two different ways.
 * This interface is that cycle.
 *
 * <h2>Finishing is not one single thing</h2>
 *
 * <p>{@link #stopCellEditing} accepts the value and <strong>may refuse</strong>: it returns
 * {@code false} if what was typed is not valid, and the editing stays open with the focus where
 * it was. {@link #cancelCellEditing} discards and cannot fail.
 *
 * <p>That asymmetry is what allows validating without losing what the user typed. A single
 * {@code stop} method that could not refuse would force one to accept rubbish or to erase it.
 *
 * <h2>{@link #isCellEditable} receives the event, not the cell</h2>
 *
 * <p>Because the question is not "can this cell be edited?" but "does <em>this gesture</em>
 * start an editing?". A single click chooses and two edit, and only the event tells the two
 * cases apart.
 */
public interface CellEditor {

    /** What the user left written. */
    Object getCellEditorValue();

    /** Whether {@code anEvent} should start an editing; see the interface note. */
    boolean isCellEditable(EventObject anEvent);

    /** Whether the gesture that starts the editing should also choose the cell. */
    boolean shouldSelectCell(EventObject anEvent);

    /** It finishes accepting; {@code false} if the value is not valid and the editing goes on. */
    boolean stopCellEditing();

    /** It finishes discarding. It cannot fail. */
    void cancelCellEditing();

    /** It adds a listener. */
    void addCellEditorListener(CellEditorListener l);

    /** It removes a listener. */
    void removeCellEditorListener(CellEditorListener l);
}
