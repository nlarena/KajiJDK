package javax.swing.table;

import java.io.Serializable;
import java.util.Vector;

import javax.swing.event.TableModelEvent;

/**
 * A table model made of vectors, for when writing one is not worth it.
 *
 * <h2>Everything is {@code Object} and everything is editable</h2>
 *
 * <p>It keeps a vector of rows, each one a vector of cells, and knows nothing about types: every
 * column is of {@link Object} and every cell can be edited. It is what makes it convenient to
 * start with and what makes it insufficient as soon as the table has to sort numbers or show
 * check marks -- there a subclass that says {@code getColumnClass} is better.
 *
 * <h2>The rows stretch themselves</h2>
 *
 * <p>Adding a row shorter than the columns is not an error: it is padded with nulls. And adding a
 * column stretches every row. The structure is kept rectangular without the caller having to look
 * after it, which is half the reason this class exists.
 *
 * <h2>The three oddly named methods</h2>
 *
 * <p>{@link #newDataAvailable}, {@link #newRowsAdded} and {@link #rowsRemoved} take an event and
 * report. They come from an old version of Swing where the caller touched the vector directly and
 * reported afterwards; they are still there for compatibility and there is no reason to use them
 * today.
 */
public class DefaultTableModel extends AbstractTableModel implements Serializable {

    /** The rows; each one a vector of cells. */
    protected Vector<Vector> dataVector;

    /** The column names. */
    protected Vector columnIdentifiers;

    /** With no rows or columns. */
    public DefaultTableModel() {
        this(0, 0);
    }

    /** With that many rows and columns, all empty. */
    public DefaultTableModel(int rowCount, int columnCount) {
        this(newVector(columnCount), rowCount);
    }

    /**
     * With those column names and that many empty rows.
     *
     * <p>A negative count blows up, but not here: the error comes out of the vector that is
     * attempted, with its message. It is what the JDK does and it is measured.
     *
     * @throws IllegalArgumentException if the row count is negative
     */
    public DefaultTableModel(Vector<?> columnNames, int rowCount) {
        setDataVector(newVector(rowCount), columnNames);
    }

    /** The same, with the names in an array. */
    public DefaultTableModel(Object[] columnNames, int rowCount) {
        this(convertToVector(columnNames), rowCount);
    }

    /** With that data and those names. */
    public DefaultTableModel(Vector<? extends Vector> data, Vector<?> columnNames) {
        setDataVector(data, columnNames);
    }

    /** The same, with arrays. */
    public DefaultTableModel(Object[][] data, Object[] columnNames) {
        setDataVector(data, columnNames);
    }

    /** The data; not a copy -- touching it changes the model without anybody finding out. */
    public Vector<Vector> getDataVector() {
        return dataVector;
    }

    /**
     * Replaces data and column names.
     *
     * <p>It reports a change of structure, not of data: the columns change and the table has to
     * rebuild them. See {@link AbstractTableModel}'s note.
     */
    public void setDataVector(Vector<? extends Vector> dataVector, Vector<?> columnIdentifiers) {
        this.dataVector = new Vector<Vector>(0);
        this.columnIdentifiers = nonNullVector(columnIdentifiers);
        if (dataVector != null) {
            for (int i = 0; i < dataVector.size(); i++) {
                this.dataVector.addElement(dataVector.elementAt(i));
            }
        }
        justifyRows(0, getRowCount());
        fireTableStructureChanged();
    }

    /** The same, with arrays. */
    public void setDataVector(Object[][] dataVector, Object[] columnIdentifiers) {
        setDataVector(convertToVector(dataVector), convertToVector(columnIdentifiers));
    }

    /** Reports that the data changed; see the class note. */
    public void newDataAvailable(TableModelEvent event) {
        fireTableChanged(event);
    }

    /**
     * Reports that rows were added, after evening them out.
     *
     * <p>Evening them out is what keeps a row shorter than the columns from breaking anything.
     */
    public void newRowsAdded(TableModelEvent e) {
        justifyRows(e.getFirstRow(), e.getLastRow() + 1);
        fireTableChanged(e);
    }

    /** Reports that rows were removed. */
    public void rowsRemoved(TableModelEvent event) {
        fireTableChanged(event);
    }

    /**
     * How many rows there are; it adds empty ones or removes the extra ones.
     *
     * @deprecated Use {@link #setRowCount}.
     */
    @Deprecated
    public void setNumRows(int rowCount) {
        int old = getRowCount();
        if (old == rowCount) {
            return;
        }
        dataVector.setSize(rowCount);
        if (rowCount <= old) {
            fireTableRowsDeleted(rowCount, old - 1);
        } else {
            justifyRows(old, rowCount);
            fireTableRowsInserted(old, rowCount - 1);
        }
    }

    /** How many rows there are; it adds empty ones or removes the extra ones. */
    public void setRowCount(int rowCount) {
        setNumRows(rowCount);
    }

    /** Adds a row at the end. */
    public void addRow(Vector<?> rowData) {
        insertRow(getRowCount(), rowData);
    }

    /** The same, with an array. */
    public void addRow(Object[] rowData) {
        addRow(convertToVector(rowData));
    }

    /**
     * Puts a row at that position.
     *
     * @throws ArrayIndexOutOfBoundsException if the position is out of range
     */
    public void insertRow(int row, Vector<?> rowData) {
        dataVector.insertElementAt(nonNullVector(rowData), row);
        justifyRows(row, row + 1);
        fireTableRowsInserted(row, row);
    }

    /** The same, with an array. */
    public void insertRow(int row, Object[] rowData) {
        insertRow(row, convertToVector(rowData));
    }

    /**
     * Moves the block of rows from {@code start} to {@code end} so that it starts at {@code to}.
     *
     * @throws ArrayIndexOutOfBoundsException if some index is out of range
     */
    public void moveRow(int start, int end, int to) {
        int shift = to - start;
        int first;
        int last;
        if (shift < 0) {
            first = to;
            last = end;
        } else {
            first = start;
            last = to + end - start;
        }
        verifyRange(first, last);
        // The stretch from the first to the last affected is rotated, not the block that moves:
        // what
                // leaves one end has to come in at the other.
        rotate(dataVector, first, last + 1, to - start);
        fireTableRowsUpdated(first, last);
    }

    private void verifyRange(int first, int last) {
        if (first < 0 || last >= getRowCount()) {
            throw new ArrayIndexOutOfBoundsException("Range out of bounds");
        }
    }

    /**
     * Rotates a stretch of the vector.
     *
     * <p>Moving a block is rotating: what leaves one side comes in at the other, with no extra
     * room.
     */
    private static void rotate(Vector<Vector> v, int a, int b, int shift) {
        int size = b - a;
        int r = size - shift;
        int g = gcd(size, r);
        for (int i = 0; i < g; i++) {
            int to = i;
            Vector tmp = v.elementAt(a + to);
            for (int from = (to + r) % size; from != i; from = (to + r) % size) {
                v.setElementAt(v.elementAt(a + from), a + to);
                to = from;
            }
            v.setElementAt(tmp, a + to);
        }
    }

    private static int gcd(int i, int j) {
        return (j == 0) ? i : gcd(j, i % j);
    }

    /**
     * Removes that row.
     *
     * @throws ArrayIndexOutOfBoundsException if the position is out of range
     */
    public void removeRow(int row) {
        dataVector.removeElementAt(row);
        fireTableRowsDeleted(row, row);
    }

    /**
     * Changes the column names, and with them how many there are.
     *
     * <p>Null leaves zero columns.
     */
    public void setColumnIdentifiers(Vector<?> columnIdentifiers) {
        setDataVector(dataVector, columnIdentifiers);
    }

    /** The same, with an array. */
    public void setColumnIdentifiers(Object[] newIdentifiers) {
        setColumnIdentifiers(convertToVector(newIdentifiers));
    }

    /** How many columns there are; it adds nameless ones or removes the extra ones. */
    public void setColumnCount(int columnCount) {
        columnIdentifiers.setSize(columnCount);
        justifyRows(0, getRowCount());
        fireTableStructureChanged();
    }

    /** Adds an empty column with that name. */
    public void addColumn(Object columnName) {
        addColumn(columnName, (Vector) null);
    }

    /**
     * Adds a column with those values.
     *
     * <p>If there are fewer values than rows, the leftover ones are left null.
     */
    public void addColumn(Object columnName, Vector columnData) {
        columnIdentifiers.addElement(columnName);
        if (columnData != null) {
            int columnSize = columnData.size();
            if (columnSize > getRowCount()) {
                dataVector.setSize(columnSize);
            }
            justifyRows(0, getRowCount());
            int newColumn = getColumnCount() - 1;
            for (int i = 0; i < columnSize; i++) {
                Vector row = dataVector.elementAt(i);
                row.setElementAt(columnData.elementAt(i), newColumn);
            }
        } else {
            justifyRows(0, getRowCount());
        }
        fireTableStructureChanged();
    }

    /** The same, with an array. */
    public void addColumn(Object columnName, Object[] columnData) {
        addColumn(columnName, convertToVector(columnData));
    }

    public int getRowCount() {
        return dataVector.size();
    }

    public int getColumnCount() {
        return columnIdentifiers.size();
    }

    /**
     * That column's name.
     *
     * <p>A column with no name set is called whatever {@link AbstractTableModel} says: A, B, C...
     */
    public String getColumnName(int column) {
        Object id = null;
        if (column < columnIdentifiers.size() && (column >= 0)) {
            id = columnIdentifiers.elementAt(column);
        }
        return (id == null) ? super.getColumnName(column) : id.toString();
    }

    /** Always true: see the class note. */
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    /**
     * @throws ArrayIndexOutOfBoundsException if the row or the column are out of range
     */
    public Object getValueAt(int row, int column) {
        Vector rowVector = dataVector.elementAt(row);
        return rowVector.elementAt(column);
    }

    /**
     * @throws ArrayIndexOutOfBoundsException if the row or the column are out of range
     */
    public void setValueAt(Object aValue, int row, int column) {
        Vector rowVector = dataVector.elementAt(row);
        rowVector.setElementAt(aValue, column);
        fireTableCellUpdated(row, column);
    }

    /** That array as a vector; null gives an empty vector. */
    protected static Vector<Object> convertToVector(Object[] anArray) {
        if (anArray == null) {
            return null;
        }
        Vector<Object> v = new Vector<Object>(anArray.length);
        for (int i = 0; i < anArray.length; i++) {
            v.addElement(anArray[i]);
        }
        return v;
    }

    /** That matrix as a vector of vectors; null gives null. */
    protected static Vector<Vector<Object>> convertToVector(Object[][] anArray) {
        if (anArray == null) {
            return null;
        }
        Vector<Vector<Object>> v = new Vector<Vector<Object>>(anArray.length);
        for (int i = 0; i < anArray.length; i++) {
            v.addElement(convertToVector(anArray[i]));
        }
        return v;
    }

    /** Evens those rows out to the columns' width, padding with nulls. */
    private void justifyRows(int from, int to) {
        dataVector.setSize(getRowCount());
        for (int i = from; i < to; i++) {
            if (dataVector.elementAt(i) == null) {
                dataVector.setElementAt(new Vector<Object>(), i);
            }
            dataVector.elementAt(i).setSize(getColumnCount());
        }
    }

    /**
     * A vector of that length, full of nulls.
     *
     * <p>Raw on purpose: it serves both for a list of column names and for a list of rows, which
     * are two different types. It is as it is in the JDK.
     */
    @SuppressWarnings("rawtypes")
    private static Vector newVector(int size) {
        Vector v = new Vector(size);
        v.setSize(size);
        return v;
    }

    /**
     * A copy of that vector, or an empty one if it is null.
     *
     * <p>It is copied with {@code addAll} and not with the copy constructor, which is what the JDK
     * does: this compiler does not accept a wildcard argument in a generic class's constructor --
     * see finding #519 -- and through a method it does go.
     */
    @SuppressWarnings("rawtypes")
    private static Vector nonNullVector(Vector<?> v) {
        Vector<Object> copy = new Vector<Object>();
        if (v != null) {
            copy.addAll(v);
        }
        return copy;
    }
}
