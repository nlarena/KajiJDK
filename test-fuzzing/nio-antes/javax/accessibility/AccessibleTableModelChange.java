package javax.accessibility;

/**
 * What changed in an accessible table.
 *
 * <p>It describes the change as a **rectangle** of rows and columns plus the kind of change,
 * instead of sending the whole table. It is what lets an assistive technology follow a large
 * spreadsheet without rereading it on each modification.
 */
public interface AccessibleTableModelChange {

    /** Rows or columns were inserted. */
    int INSERT = 1;

    /** The contents changed. */
    int UPDATE = 0;

    /** Rows or columns were deleted. */
    int DELETE = -1;

    /** `INSERT`, `UPDATE` or `DELETE`. */
    int getType();

    /** The first row affected. */
    int getFirstRow();

    /** The last row affected. */
    int getLastRow();

    /** The first column affected. */
    int getFirstColumn();

    /** The last column affected. */
    int getLastColumn();
}
