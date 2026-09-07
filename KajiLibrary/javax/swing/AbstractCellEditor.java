package javax.swing;

import java.io.Serializable;
import java.util.EventObject;

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;

/**
 * La parte de un editor de celda que es igual en todos: los oyentes y los dos avisos.
 *
 * <h2>Terminar y cancelar no son lo mismo</h2>
 *
 * <p>{@link #stopCellEditing} dice "quedate con lo que escribi" y devuelve {@code false} si el
 * editor no puede aceptarlo -- un texto que no es un numero, por ejemplo --, en cuyo caso la edicion
 * sigue abierta. {@link #cancelCellEditing} dice "olvidate", no devuelve nada y no puede fallar.
 *
 * <p>Esta clase implementa las dos de la forma mas simple posible: terminar siempre se puede.
 * Validar es de la subclase.
 *
 * <h2>Los oyentes se recorren al reves</h2>
 *
 * <p>De atras para adelante, como en todo Swing: el ultimo en anotarse es el primero en enterarse.
 */
public abstract class AbstractCellEditor implements CellEditor, Serializable {

    /** Los oyentes, por tipo. */
    protected EventListenerList listenerList = new EventListenerList();

    /** El evento, armado una vez y reusado. */
    protected transient ChangeEvent changeEvent = null;

    /** Para las subclases. */
    protected AbstractCellEditor() {
    }

    /** Siempre cierto: cualquier gesto empieza a editar. La subclase decide otra cosa. */
    public boolean isCellEditable(EventObject e) {
        return true;
    }

    /** Siempre cierto: empezar a editar tambien elige la celda. */
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    /** Termina y avisa; siempre se puede. Ver la nota de la clase. */
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    /** Cancela y avisa. */
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

    /** Avisa que la edicion termino bien. */
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

    /** Avisa que la edicion se cancelo. */
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
