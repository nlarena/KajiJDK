package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que una celda termino de editarse.
 */
public interface CellEditorListener extends EventListener {

    /** La edicion termino y el valor se acepta. */
    void editingStopped(ChangeEvent e);

    /** La edicion se abandono; el valor no cambia. */
    void editingCanceled(ChangeEvent e);
}
