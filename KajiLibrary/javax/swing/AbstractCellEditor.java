package javax.swing;

import java.io.Serializable;
import java.util.EventObject;

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;

/**
 * The part of a cell editor that is the same in all of them: the listeners and the two
 * notices.
 *
 * <h2>Finishing and cancelling are not the same</h2>
 *
 * <p>{@link #stopCellEditing} says "keep what I typed" and returns {@code false} if the
 * editor cannot accept it -- a text that is not a number, for instance --, in which case the
 * editing stays open. {@link #cancelCellEditing} says "forget it", returns nothing and cannot
 * fail.
 *
 * <p>This class implements both in the simplest way possible: finishing is always possible.
 * Validating belongs to the subclass.
 *
 * <h2>The listeners are walked through backwards</h2>
 *
 * <p>Back to front, as everywhere in Swing: the last to sign up is the first to learn about
 * it.
 */
public abstract class AbstractCellEditor implements CellEditor, Serializable {

    /** The listeners, by type. */
    protected EventListenerList listenerList = new EventListenerList();

    /** The event, built once and reused. */
    protected transient ChangeEvent changeEvent = null;

    /** For the subclasses. */
    protected AbstractCellEditor() {
    }

    /** Always true: any gesture starts editing. The subclass decides otherwise. */
    public boolean isCellEditable(EventObject e) {
        return true;
    }

    /** Always true: starting to edit also chooses the cell. */
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    /** It finishes and gives notice; it is always possible. See the class note. */
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    /** It cancels and gives notice. */
    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    public void addCellEditorListener(CellEditorListener l) {
        listenerList.add(CellEditorListener.class, l);
    }

    public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
    }

    public CellEditorListener[] getCellEditorListeners() {
        return listenerList.getListeners(CellEditorListener.class);
    }

    /** It gives notice that the editing finished well. */
    protected void fireEditingStopped() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == CellEditorListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((CellEditorListener) listeners[i + 1]).editingStopped(changeEvent);
            }
        }
    }

    /** It gives notice that the editing was cancelled. */
    protected void fireEditingCanceled() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == CellEditorListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((CellEditorListener) listeners[i + 1]).editingCanceled(changeEvent);
            }
        }
    }
}
