package javax.accessibility;

/**
 * Implemented by what shows data in **rows and columns**.
 *
 * <p>What separates it from a list of lists are the headers and the descriptions: without them,
 * whoever does not see the screen hears a loose number and has no way of knowing which column it
 * belongs to. The headers are tables in turn, which allows multi-level headers.
 *
 * <p>The **extent** of a cell --{@link #getAccessibleRowExtentAt} and its pair-- is how many rows
 * or columns it spans. It is what makes a merged cell be announced as one and not as four repeated
 * cells.
 */
public interface AccessibleTable {

    /** The table's caption, or `null` if it has none. */
    Accessible getAccessibleCaption();

    /** Changes the caption. */
    void setAccessibleCaption(Accessible a);

    /** The table's summary, or `null` if it has none. */
    Accessible getAccessibleSummary();

    /** Changes the summary. */
    void setAccessibleSummary(Accessible a);

    /** How many rows there are. */
    int getAccessibleRowCount();

    /** How many columns there are. */
    int getAccessibleColumnCount();

    /**
     * The cell at that position.
     *
     * @return the cell, or `null` if the position does not exist
     */
    Accessible getAccessibleAt(int r, int c);

    /** How many rows that cell spans. */
    int getAccessibleRowExtentAt(int r, int c);

    /** How many columns that cell spans. */
    int getAccessibleColumnExtentAt(int r, int c);

    /** The row header, itself a table, or `null` if there is none. */
    AccessibleTable getAccessibleRowHeader();

    /** Changes the row header. */
    void setAccessibleRowHeader(AccessibleTable table);

    /** The column header, or `null` if there is none. */
    AccessibleTable getAccessibleColumnHeader();

    /** Changes the column header. */
    void setAccessibleColumnHeader(AccessibleTable table);

    /** What describes that row, or `null` if nothing. */
    Accessible getAccessibleRowDescription(int r);

    /** Changes that row's description. */
    void setAccessibleRowDescription(int r, Accessible a);

    /** What describes that column, or `null` if nothing. */
    Accessible getAccessibleColumnDescription(int c);

    /** Changes that column's description. */
    void setAccessibleColumnDescription(int c, Accessible a);

    /** Whether that cell is chosen. */
    boolean isAccessibleSelected(int r, int c);

    /** Whether that whole row is chosen. */
    boolean isAccessibleRowSelected(int r);

    /** Whether that whole column is chosen. */
    boolean isAccessibleColumnSelected(int c);

    /** Which rows are chosen. */
    int[] getSelectedAccessibleRows();

    /** Which columns are chosen. */
    int[] getSelectedAccessibleColumns();
}
