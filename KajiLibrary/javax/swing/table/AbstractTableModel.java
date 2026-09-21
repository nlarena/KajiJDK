package javax.swing.table;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * What every table model shares: the listeners and the notices.
 *
 * <h2>What has to be written is little</h2>
 *
 * <p>A subclass only has to give {@code getRowCount}, {@code getColumnCount} and
 * {@code getValueAt}. Everything else has a reasonable answer: the column names are A, B, C...,
 * every column's type is {@link Object}, and nothing can be edited.
 *
 * <h2>The six notices</h2>
 *
 * <p>{@link #fireTableDataChanged}, {@link #fireTableStructureChanged} and the four by range.
 * The difference that matters is between the first two: <strong>"the data changed" keeps the
 * columns; "the structure changed" throws them away</strong> and the table builds them again
 * from scratch. Using the second when the first would have done wipes out the widths the user
 * had adjusted by hand, and it is the commonest mistake with this class.
 *
 * <p>The listeners are walked from the back to the front, as everywhere in Swing.
 */
public abstract class AbstractTableModel implements TableModel, Serializable {

    /** The listeners, by type. */
    protected EventListenerList listenerList = new EventListenerList();

    /** For the subclasses. */
    protected AbstractTableModel() {
    }

    /**
     * That column's name: A, B, ... Z, AA, AB, ...
     *
     * <p>It is the spreadsheets' scheme, and it is what is seen when nobody set names.
     */
    public String getColumnName(int column) {
        String result = "";
        for (; column >= 0; column = column / 26 - 1) {
            result = (char) ((char) (column % 26) + 'A') + result;
        }
        return result;
    }

    /**
     * The column called that, or -1.
     *
     * <p>It compares with {@code equals}, so it distinguishes case.
     */
    public int findColumn(String columnName) {
        for (int i = 0; i < getColumnCount(); i++) {
            if (columnName.equals(getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@link Object} for every column; a subclass that knows the type says so and gains a renderer.
     */
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    /** False: nothing is edited while the subclass does not say otherwise. */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /** It does nothing; the subclass that allows editing has to write it. */
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

    /** The data changed, not the columns; see the class note. */
    public void fireTableDataChanged() {
        fireTableChanged(new TableModelEvent(this));
    }

    /** The structure changed: the table throws its columns away and rebuilds them. */
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

    /** Hands the notice out to the listeners, from the last registered to the first. */
    public void fireTableChanged(TableModelEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableModelListener.class) {
                ((TableModelListener) listeners[i + 1]).tableChanged(e);
            }
        }
    }

    /** The listeners of that type registered on this model. */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
