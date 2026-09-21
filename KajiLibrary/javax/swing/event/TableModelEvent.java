package javax.swing.event;

import java.util.EventObject;

import javax.swing.table.TableModel;

/**
 * A table's data changed.
 *
 * <h2>An event that is read by its constants</h2>
 *
 * <p>The combination of range, column and type covers everything from "one cell changed" to
 * "everything changed", and the two special constants are what make the general case practical:
 * {@link #HEADER_ROW} as the first row means the <strong>structure</strong> changed --there are
 * other columns, not other data-- and {@link #ALL_COLUMNS} that the change spans the whole row.
 *
 * <p>The distinction matters because a change of structure forces the table to redo its columns,
 * and one of data only to repaint. Confusing them is the difference between a table that flickers
 * and one that shows old columns.
 */
public class TableModelEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** Rows were inserted. */
    public static final int INSERT = 1;

    /** Values changed. */
    public static final int UPDATE = 0;

    /** Rows were deleted. */
    public static final int DELETE = -1;

    /** As the first row: the column structure changed. */
    public static final int HEADER_ROW = -1;

    /** As the column: the change spans them all. */
    public static final int ALL_COLUMNS = -1;

    protected int type;
    protected int firstRow;
    protected int lastRow;
    protected int column;

    /** Everything changed. */
    public TableModelEvent(TableModel source) {
        this(source, 0, Integer.MAX_VALUE, ALL_COLUMNS, UPDATE);
    }

    /** A whole row changed. */
    public TableModelEvent(TableModel source, int row) {
        this(source, row, row, ALL_COLUMNS, UPDATE);
    }

    /** A range of rows changed. */
    public TableModelEvent(TableModel source, int firstRow, int lastRow) {
        this(source, firstRow, lastRow, ALL_COLUMNS, UPDATE);
    }

    /** One column of a range of rows changed. */
    public TableModelEvent(TableModel source, int firstRow, int lastRow, int column) {
        this(source, firstRow, lastRow, column, UPDATE);
    }

    /** With everything explicit. */
    public TableModelEvent(TableModel source, int firstRow, int lastRow, int column, int type) {
        super(source);
        this.firstRow = firstRow;
        this.lastRow = lastRow;
        this.column = column;
        this.type = type;
    }

    /** The first row affected; {@link #HEADER_ROW} if the structure changed. */
    public int getFirstRow() {
        return this.firstRow;
    }

    /** The last row affected, inclusive. */
    public int getLastRow() {
        return this.lastRow;
    }

    /** The column affected, or {@link #ALL_COLUMNS}. */
    public int getColumn() {
        return this.column;
    }

    /** {@link #INSERT}, {@link #UPDATE} or {@link #DELETE}. */
    public int getType() {
        return this.type;
    }
}
