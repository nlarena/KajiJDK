package javax.sql.rowset;

import java.sql.SQLException;

/**
 * A {@link WebRowSet} with a filter on: only the rows the filter accepts are seen.
 *
 * <h2>Why filter on the client and not in the query</h2>
 *
 * <p>Because the set is already disconnected. Changing the {@code WHERE} would force going back to
 * the database; putting a filter on is immediate and does not cost a connection. To walk the same
 * data with several criteria —what an interface with sortable columns and search boxes does— it is
 * the difference between one query per interaction and none.
 *
 * <h2>The filter hides, it does not delete</h2>
 *
 * <p>The rows that do not pass are still there. Removing the filter shows them again, and
 * {@link CachedRowSet#acceptChanges} synchronizes <strong>all</strong> the modified ones, including
 * the ones the filter was hiding. Thinking of it as a deletion leads to losing track of changes and
 * to being surprised when they show up in the database.
 *
 * @since 1.5
 */
public interface FilteredRowSet extends WebRowSet {

    /**
     * Puts on or changes the filter.
     *
     * @param p the filter, or {@code null} to remove it
     * @throws SQLException if it could not be applied
     */
    void setFilter(Predicate p) throws SQLException;

    /**
     * The filter that is on.
     *
     * @return the filter, or {@code null} if there is none
     */
    Predicate getFilter();
}
