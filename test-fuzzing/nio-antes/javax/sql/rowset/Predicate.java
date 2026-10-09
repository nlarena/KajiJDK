package javax.sql.rowset;

import java.sql.SQLException;

import javax.sql.RowSet;

/**
 * The filter of a {@link FilteredRowSet}: it decides which rows are seen.
 *
 * <h2>Why there are three methods</h2>
 *
 * <p>Because filtering happens at two moments. {@link #evaluate(RowSet)} is called with the set
 * positioned on a row and decides whether that row is shown: it is the <strong>read</strong>
 * filter.
 *
 * <p>The other two receive a loose value and the column where it is going to be written, and they
 * are the <strong>write</strong> filter: they answer whether that value <em>would still</em> belong
 * to the filtered set. They serve to reject an insertion or a modification that would make the row
 * disappear from the very filter that contains it — a row that is written and instantly becomes
 * invisible is almost always a mistake of whoever writes it.
 *
 * <h2>A filter is a cut, not a query</h2>
 *
 * <p>The set still has all the rows: the filter only hides the ones that do not pass. Removing it
 * shows them again, without querying the source again. It is the difference from changing the
 * query's {@code WHERE}, which would force reconnecting.
 *
 * @since 1.5
 */
public interface Predicate {

    /**
     * Whether the set's current row passes the filter.
     *
     * @param rs the set, positioned on the row to evaluate
     * @return {@code true} if the row is shown
     */
    boolean evaluate(RowSet rs);

    /**
     * Whether that value would be acceptable in that column.
     *
     * @param value the value
     * @param column the column, from 1
     * @return {@code true} if the value passes the filter
     * @throws SQLException if the column does not exist
     */
    boolean evaluate(Object value, int column) throws SQLException;

    /**
     * Whether that value would be acceptable in that column.
     *
     * @param value the value
     * @param columnName the name of the column
     * @return {@code true} if the value passes the filter
     * @throws SQLException if the column does not exist
     */
    boolean evaluate(Object value, String columnName) throws SQLException;
}
