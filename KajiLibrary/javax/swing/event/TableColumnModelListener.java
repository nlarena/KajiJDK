package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que las columnas de una tabla cambiaron.
 *
 * <p>Los cinco metodos no reciben el mismo tipo de evento, y eso es deliberado: agregar, sacar y
 * mover columnas son cambios de <em>estructura</em> y llegan como {@link TableColumnModelEvent},
 * mientras que el margen es un {@link ChangeEvent} —no hay indices que informar— y la seleccion es
 * un {@link ListSelectionEvent}, el mismo que usa cualquier lista.
 *
 * <p>Reusar esos dos ultimos en vez de inventar eventos propios es lo que permite que un mismo
 * oyente de seleccion sirva para filas y para columnas.
 */
public interface TableColumnModelListener extends EventListener {

    /** Se agrego una columna. */
    void columnAdded(TableColumnModelEvent e);

    /** Se saco una columna. */
    void columnRemoved(TableColumnModelEvent e);

    /** Se movio una columna. */
    void columnMoved(TableColumnModelEvent e);

    /** Cambio el espacio entre columnas. */
    void columnMarginChanged(ChangeEvent e);

    /** Cambio que columnas estan seleccionadas. */
    void columnSelectionChanged(ListSelectionEvent e);
}
