package javax.swing.event;

import java.util.EventObject;

import javax.swing.table.TableColumnModel;

/**
 * Las columnas de una tabla cambiaron: se agrego, se saco o se movio una.
 *
 * <p>Los dos indices se leen distinto segun que paso, y es la trampa de la clase: al mover, son
 * de donde y hacia donde; al agregar o sacar, son el mismo numero repetido. Cual de los tres fue lo
 * dice el metodo del {@link TableColumnModelListener} al que llega, no el evento.
 */
public class TableColumnModelEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** De donde. */
    protected int fromIndex;

    /** Hacia donde. */
    protected int toIndex;

    public TableColumnModelEvent(TableColumnModel source, int from, int to) {
        super(source);
        this.fromIndex = from;
        this.toIndex = to;
    }

    /** De donde salio la columna. */
    public int getFromIndex() {
        return this.fromIndex;
    }

    /** Adonde fue. */
    public int getToIndex() {
        return this.toIndex;
    }
}
