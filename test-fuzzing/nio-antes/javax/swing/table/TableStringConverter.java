package javax.swing.table;

/**
 * Converts a cell's value to text, for sorting and filtering.
 *
 * <h2>Why {@code toString} is not enough</h2>
 *
 * <p>An object's {@code toString} is for the programmer; what the table shows is for the user,
 * and many times they are not the same -- a date, an amount, an enum with translated names --.
 * Sorting or filtering by the first gives a result that does not look like what is seen.
 *
 * <p>A converter of one's own solves that without touching the model: the model goes on keeping
 * objects and the view sorts them by how they read.
 */
public abstract class TableStringConverter {

    /** For the subclasses. */
    protected TableStringConverter() {
    }

    /**
     * That cell's text.
     *
     * <p>The indices are the <strong>model</strong>'s, not the view's: it is called while the order
     * is being decided, when the view's do not yet exist.
     */
    public abstract String toString(TableModel model, int row, int column);
}
