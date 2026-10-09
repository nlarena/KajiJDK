package javax.swing.table;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

/**
 * A table's columns: which ones there are, in what order and how much they measure.
 *
 * <h2>The view's columns are not the model's</h2>
 *
 * <p>This model keeps the columns <em>as they are seen</em>: they can be moved, removed and
 * repeated without the data model finding out. Each {@link TableColumn} knows which model column
 * it takes its values from, and that is why moving a column around moves no data.
 *
 * <h2>The total width is kept</h2>
 *
 * <p>Adding the widths up on every repaint would be expensive with many columns, so the total is
 * kept and recomputed when something changes. Hence this model listens to each column's property
 * changes: a column that changes width has to report it, and the one that adds up is this one.
 *
 * <h2>It also keeps the column selection</h2>
 *
 * <p>With a {@link ListSelectionModel}, the same type a list uses. That the column selection
 * lives here and not in the table is what allows two tables to share columns and selection.
 */
public class DefaultTableColumnModel implements TableColumnModel, PropertyChangeListener,
        ListSelectionListener, Serializable {

    /** The columns, in the order they are seen. */
    protected Vector<TableColumn> tableColumns;

    /** The column selection. */
    protected ListSelectionModel selectionModel;

    /** The space between one column and the next. */
    protected int columnMargin;

    /** The listeners, by type. */
    protected EventListenerList listenerList = new EventListenerList();

    /** The margin change event, built once. */
    protected transient ChangeEvent changeEvent = null;

    /** Whether columns can be chosen. */
    protected boolean columnSelectionAllowed;

    /** The sum of the widths, kept; see the class note. */
    protected int totalColumnWidth;

    /** With no columns, a margin of one and no column selection. */
    public DefaultTableColumnModel() {
        super();
        tableColumns = new Vector<TableColumn>();
        setSelectionModel(createSelectionModel());
        setColumnMargin(1);
        invalidateWidthCache();
        setColumnSelectionAllowed(false);
    }

    /**
     * Adds a column at the end.
     *
     * @throws IllegalArgumentException if it is null
     */
    public void addColumn(TableColumn aColumn) {
        if (aColumn == null) {
            throw new IllegalArgumentException("Object is null");
        }
        tableColumns.addElement(aColumn);
        aColumn.addPropertyChangeListener(this);
        invalidateWidthCache();
        fireColumnAdded(new TableColumnModelEvent(this, 0, getColumnCount() - 1));
    }

    /**
     * Removes that column.
     *
     * <p>A column that is not there is ignored silently: removing something that was not there
     * leaves the model the same, which is what the caller wanted.
     */
    public void removeColumn(TableColumn column) {
        int columnIndex = tableColumns.indexOf(column);
        if (columnIndex != -1) {
            if (selectionModel != null) {
                selectionModel.removeIndexInterval(columnIndex, columnIndex);
            }
            column.removePropertyChangeListener(this);
            tableColumns.removeElementAt(columnIndex);
            invalidateWidthCache();
            fireColumnRemoved(new TableColumnModelEvent(this, columnIndex, 0));
        }
    }

    /**
     * Moves a column to another position.
     *
     * <p>The selection moves with it: otherwise, dragging a chosen column would leave the one that
     * took its place chosen.
     *
     * @throws IllegalArgumentException if some index is out of range
     */
    public void moveColumn(int columnIndex, int newIndex) {
        if ((columnIndex < 0) || (columnIndex >= getColumnCount())
                || (newIndex < 0) || (newIndex >= getColumnCount())) {
            throw new IllegalArgumentException("moveColumn() - Index out of range");
        }
        if (columnIndex == newIndex) {
            fireColumnMoved(new TableColumnModelEvent(this, columnIndex, newIndex));
            return;
        }
        TableColumn aColumn = tableColumns.elementAt(columnIndex);
        tableColumns.removeElementAt(columnIndex);
        boolean selected = selectionModel.isSelectedIndex(columnIndex);
        selectionModel.removeIndexInterval(columnIndex, columnIndex);
        tableColumns.insertElementAt(aColumn, newIndex);
        selectionModel.insertIndexInterval(newIndex, 1, true);
        if (selected) {
            selectionModel.addSelectionInterval(newIndex, newIndex);
        } else {
            selectionModel.removeSelectionInterval(newIndex, newIndex);
        }
        fireColumnMoved(new TableColumnModelEvent(this, columnIndex, newIndex));
    }

    /** The space between columns; it changes the total width. */
    public void setColumnMargin(int newMargin) {
        if (newMargin != columnMargin) {
            columnMargin = newMargin;
            fireColumnMarginChanged();
        }
    }

    public int getColumnCount() {
        return tableColumns.size();
    }

    public Enumeration<TableColumn> getColumns() {
        return tableColumns.elements();
    }

    /**
     * The first column with that identifier.
     *
     * @throws IllegalArgumentException if the identifier is null or there is none
     */
    public int getColumnIndex(Object identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Identifier is null");
        }
        int index = 0;
        Enumeration<TableColumn> e = getColumns();
        while (e.hasMoreElements()) {
            TableColumn aColumn = e.nextElement();
            if (identifier.equals(aColumn.getIdentifier())) {
                return index;
            }
            index = index + 1;
        }
        throw new IllegalArgumentException("Identifier not found");
    }

    public TableColumn getColumn(int columnIndex) {
        return tableColumns.elementAt(columnIndex);
    }

    public int getColumnMargin() {
        return columnMargin;
    }

    /**
     * The column that occupies that pixel.
     *
     * <p>A pixel to the left of the first or to the right of the last returns -1: there is no
     * column there, and returning the nearest one would make a click outside the table choose
     * something.
     */
    public int getColumnIndexAtX(int x) {
        if (x < 0) {
            return -1;
        }
        int cc = getColumnCount();
        for (int column = 0; column < cc; column++) {
            x = x - getColumn(column).getWidth();
            if (x < 0) {
                return column;
            }
        }
        return -1;
    }

    /** The sum of the widths, plus the margins. */
    public int getTotalColumnWidth() {
        if (totalColumnWidth == -1) {
            recalcWidthCache();
        }
        return totalColumnWidth;
    }

    /**
     * Changes the column selection model.
     *
     * @throws IllegalArgumentException if it is null
     */
    public void setSelectionModel(ListSelectionModel newModel) {
        if (newModel == null) {
            throw new IllegalArgumentException("Cannot set a null SelectionModel");
        }
        ListSelectionModel oldModel = selectionModel;
        if (newModel != oldModel) {
            if (oldModel != null) {
                oldModel.removeListSelectionListener(this);
            }
            selectionModel = newModel;
            newModel.addListSelectionListener(this);
        }
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setColumnSelectionAllowed(boolean flag) {
        columnSelectionAllowed = flag;
    }

    public boolean getColumnSelectionAllowed() {
        return columnSelectionAllowed;
    }

    /** The chosen columns; an empty array if there are none or they cannot be chosen. */
    public int[] getSelectedColumns() {
        if (selectionModel != null) {
            int iMin = selectionModel.getMinSelectionIndex();
            int iMax = selectionModel.getMaxSelectionIndex();
            if ((iMin == -1) || (iMax == -1)) {
                return new int[0];
            }
            int[] rvTmp = new int[1 + (iMax - iMin)];
            int n = 0;
            for (int i = iMin; i <= iMax; i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    rvTmp[n] = i;
                    n = n + 1;
                }
            }
            int[] rv = new int[n];
            System.arraycopy(rvTmp, 0, rv, 0, n);
            return rv;
        }
        return new int[0];
    }

    public int getSelectedColumnCount() {
        if (selectionModel != null) {
            int iMin = selectionModel.getMinSelectionIndex();
            int iMax = selectionModel.getMaxSelectionIndex();
            int count = 0;
            for (int i = iMin; i <= iMax; i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    count = count + 1;
                }
            }
            return count;
        }
        return 0;
    }

    public void addColumnModelListener(TableColumnModelListener x) {
        listenerList.add(TableColumnModelListener.class, x);
    }

    public void removeColumnModelListener(TableColumnModelListener x) {
        listenerList.remove(TableColumnModelListener.class, x);
    }

    public TableColumnModelListener[] getColumnModelListeners() {
        return listenerList.getListeners(TableColumnModelListener.class);
    }

    protected void fireColumnAdded(TableColumnModelEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableColumnModelListener.class) {
                ((TableColumnModelListener) listeners[i + 1]).columnAdded(e);
            }
        }
    }

    protected void fireColumnRemoved(TableColumnModelEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableColumnModelListener.class) {
                ((TableColumnModelListener) listeners[i + 1]).columnRemoved(e);
            }
        }
    }

    protected void fireColumnMoved(TableColumnModelEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableColumnModelListener.class) {
                ((TableColumnModelListener) listeners[i + 1]).columnMoved(e);
            }
        }
    }

    protected void fireColumnSelectionChanged(ListSelectionEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableColumnModelListener.class) {
                ((TableColumnModelListener) listeners[i + 1]).columnSelectionChanged(e);
            }
        }
    }

    /** The event is built once and reused: it does not say what changed, only that it did. */
    protected void fireColumnMarginChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableColumnModelListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((TableColumnModelListener) listeners[i + 1]).columnMarginChanged(changeEvent);
            }
        }
    }

    /** The listeners of that type registered on this model. */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /**
     * A column changed width: it has to be added up again.
     *
     * <p>Only the width and the preferred width change the total; a column's other changes -- its
     * title, its renderer -- do not.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if ("width".equals(name) || "preferredWidth".equals(name)) {
            invalidateWidthCache();
            fireColumnMarginChanged();
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        fireColumnSelectionChanged(e);
    }

    /** The selection model used if nobody gives another. */
    protected ListSelectionModel createSelectionModel() {
        return new DefaultListSelectionModel();
    }

    /** Adds the widths up again; see the class note. */
    protected void recalcWidthCache() {
        Enumeration<TableColumn> e = getColumns();
        totalColumnWidth = 0;
        while (e.hasMoreElements()) {
            totalColumnWidth = totalColumnWidth + e.nextElement().getWidth();
        }
    }

    private void invalidateWidthCache() {
        totalColumnWidth = -1;
    }
}
