package javax.swing.table;

import javax.swing.event.TableModelListener;

/**
 * A table's data: how many rows, how many columns, and what is in each cell.
 *
 * <p>The table does not keep data -- it asks for it. That allows a table of a million rows to
 * exist without a million cells being in memory: the model computes them or fetches them when
 * they are asked for.
 *
 * <p>{@link #getColumnClass} exists so that the table knows <em>how</em> to draw each column
 * without looking at the values: one of booleans is drawn with checkboxes, one of dates with the
 * local format. If it depended on the content, a column with the first cell empty would be drawn
 * differently from the rest.
 */
public interface TableModel {

    /** How many rows there are. */
    int getRowCount();

    /** How many columns there are. */
    int getColumnCount();

    /** A column's name, for the header. */
    String getColumnName(int columnIndex);

    /** The type of a column's values; see the interface note. */
    Class<?> getColumnClass(int columnIndex);

    /** Whether that cell can be edited. */
    boolean isCellEditable(int rowIndex, int columnIndex);

    /** That cell's value. */
    Object getValueAt(int rowIndex, int columnIndex);

    /** Changes that cell's value. */
    void setValueAt(Object aValue, int rowIndex, int columnIndex);

    /** Adds a listener. */
    void addTableModelListener(TableModelListener l);

    /** Removes a listener. */
    void removeTableModelListener(TableModelListener l);
}
