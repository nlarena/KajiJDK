package javax.swing.table;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * Lo que todo modelo de tabla comparte: los oyentes y los avisos.
 *
 * <h2>Lo que hay que escribir es poco</h2>
 *
 * <p>Una subclase solo tiene que dar {@code getRowCount}, {@code getColumnCount} y
 * {@code getValueAt}. Todo lo demas tiene una respuesta razonable: los nombres de columna son A, B,
 * C..., el tipo de toda columna es {@link Object}, y nada se puede editar.
 *
 * <h2>Los seis avisos</h2>
 *
 * <p>{@link #fireTableDataChanged}, {@link #fireTableStructureChanged} y los cuatro por rango. La
 * diferencia que importa es la de los dos primeros: <strong>"cambiaron los datos" conserva las
 * columnas; "cambio la estructura" las tira</strong> y la tabla vuelve a armarlas desde cero. Usar
 * el segundo cuando alcanzaba el primero borra los anchos que el usuario habia ajustado a mano, y es
 * el error mas comun con esta clase.
 *
 * <p>Los oyentes se recorren de atras para adelante, como en todo Swing.
 */
public abstract class AbstractTableModel implements TableModel, Serializable {

    /** Los oyentes, por tipo. */
    protected EventListenerList listenerList = new EventListenerList();

    /** Para las subclases. */
    protected AbstractTableModel() {
    }

    /**
     * El nombre de esa columna: A, B, ... Z, AA, AB, ...
     *
     * <p>Es el esquema de las hojas de calculo, y es lo que se ve cuando nadie puso nombres.
     */
    public String getColumnName(int column) {
        String result = "";
        for (; column >= 0; column = column / 26 - 1) {
            result = (char) ((char) (column % 26) + 'A') + result;
        }
        return result;
    }

    /**
     * La columna que se llama asi, o -1.
     *
     * <p>Compara con {@code equals}, asi que distingue mayusculas.
     */
    public int findColumn(String columnName) {
        for (int i = 0; i < getColumnCount(); i++) {
            if (columnName.equals(getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }

    /** {@link Object} para toda columna; una subclase que sepa el tipo lo dice y gana renderer. */
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    /** Falso: nada se edita mientras la subclase no diga otra cosa. */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /** No hace nada; la subclase que permita editar tiene que escribirlo. */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    public void addTableModelListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }

    public TableModelListener[] getTableModelListeners() {
        return listenerList.getListeners(TableModelListener.class);
    }

    /** Cambiaron los datos, no las columnas; ver la nota de la clase. */
    public void fireTableDataChanged() {
        fireTableChanged(new TableModelEvent(this));
    }

    /** Cambio la estructura: la tabla tira sus columnas y las rearma. */
    public void fireTableStructureChanged() {
        fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    public void fireTableRowsInserted(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(this, firstRow, lastRow,
                TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    public void fireTableRowsUpdated(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(this, firstRow, lastRow,
                TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE));
    }

    public void fireTableRowsDeleted(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(this, firstRow, lastRow,
                TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
    }

    public void fireTableCellUpdated(int row, int column) {
        fireTableChanged(new TableModelEvent(this, row, row, column));
    }

    /** Reparte el aviso a los oyentes, del ultimo anotado al primero. */
    public void fireTableChanged(TableModelEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableModelListener.class) {
                ((TableModelListener) listeners[i + 1]).tableChanged(e);
            }
        }
    }

    /** Los oyentes de ese tipo anotados en este modelo. */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
