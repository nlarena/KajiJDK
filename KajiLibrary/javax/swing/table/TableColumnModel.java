package javax.swing.table;

import java.util.Enumeration;

import javax.swing.ListSelectionModel;
import javax.swing.event.TableColumnModelListener;

/**
 * Which columns a table has, in what order and which ones are selected.
 *
 * <h2>Separate from the data model, and that is why the table can be reordered</h2>
 *
 * <p>{@link TableModel} says which data there is; this says how it is presented. Dragging a
 * column elsewhere is {@link #moveColumn} over this model and the data one does not even find out
 * -- which is exactly what is needed, because the columns' order is a preference of the view.
 *
 * <p>Hence each {@link TableColumn} carries its own model index: the position in this list and
 * the column it takes the data from are two different numbers.
 *
 * <h2>The margin, which looks like a detail and is not</h2>
 *
 * <p>{@link #getColumnMargin} is the space between columns, and it enters into
 * {@link #getTotalColumnWidth} and {@link #getColumnIndexAtX}. Forgetting it makes the total
 * width not add up and makes a click near an edge select the neighbouring column.
 */
public interface TableColumnModel {

    /** Adds a column at the end. */
    void addColumn(TableColumn aColumn);

    /** Removes a column. */
    void removeColumn(TableColumn column);

    /** Moves a column around in the view. */
    void moveColumn(int columnIndex, int newIndex);

    /** Changes the space between columns. */
    void setColumnMargin(int newMargin);

    /** How many columns there are. */
    int getColumnCount();

    /** The columns, in view order. */
    Enumeration<TableColumn> getColumns();

    /**
     * Where the column with that identifier is.
     *
     * @throws IllegalArgumentException if there is none, or if the identifier is {@code null}
     */
    int getColumnIndex(Object columnIdentifier);

    /** The column at that position in the view. */
    TableColumn getColumn(int columnIndex);

    /** The space between columns. */
    int getColumnMargin();

    /** Which column falls on that horizontal coordinate, or {@code -1}. */
    int getColumnIndexAtX(int xPosition);

    /** The width of all the columns, counting the margins. */
    int getTotalColumnWidth();

    /** Changes whether columns can be selected. */
    void setColumnSelectionAllowed(boolean flag);

    /** Whether columns can be selected. */
    boolean getColumnSelectionAllowed();

    /** The selected columns. */
    int[] getSelectedColumns();

    /** How many columns are selected. */
    int getSelectedColumnCount();

    /** Changes the column selection model. */
    void setSelectionModel(ListSelectionModel newModel);

    /** The column selection model. */
    ListSelectionModel getSelectionModel();

    /** Adds a listener. */
    void addColumnModelListener(TableColumnModelListener x);

    /** Removes a listener. */
    void removeColumnModelListener(TableColumnModelListener x);
}
