package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a cell finished being edited.
 */
public interface CellEditorListener extends EventListener {

    /** The editing finished and the value is accepted. */
    void editingStopped(ChangeEvent e);

    /** The editing was abandoned; the value does not change. */
    void editingCanceled(ChangeEvent e);
}
