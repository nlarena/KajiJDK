package javax.sql.rowset.spi;

import java.sql.SQLException;

import javax.sql.RowSet;

/**
 * The set of rows that could <strong>not</strong> be written, to decide one by one.
 *
 * <h2>What a conflict is</h2>
 *
 * <p>The {@code RowSet} was filled at one moment and is written at another. If between those two
 * moments somebody changed the same row in the source, writing over it would lose their change
 * without anybody finding out. The provider detects that and does not write: instead of deciding on
 * its own, it gathers the rows in conflict and hands them over here.
 *
 * <h2>Why it is a {@link RowSet} and also has its own accessors</h2>
 *
 * <p>Because there are <strong>three</strong> values per cell at play: the one there was when it
 * was loaded, the one the user wrote, and the one there is now in the source.
 *
 * <p>The specification splits them like this: this object "contains the values from the data source
 * that caused the conflict(s) and null for all other values"; {@link #getConflictValue} gives that
 * source value; the value the user wanted is read from the user's own {@code RowSet}; and
 * {@link #setResolvedValue} is where the one that is finally going to stay is written. Resolving a
 * conflict is looking at the first two and choosing the third. (The note said the inherited
 * {@code RowSet} getters give the value the user wanted to put.)
 *
 * <h2>The walk</h2>
 *
 * <p>{@link #nextConflict} and {@link #previousConflict} walk only the conflicting rows, skipping
 * the ones that were written fine. {@link #getStatus} says what kind of conflict the current row
 * has — on updating, on deleting or on inserting—, which are not resolved the same way: a row that
 * no longer exists in the source cannot be updated in any way.
 *
 * @since 1.5
 */
public interface SyncResolver extends RowSet {

    /** The row that was to be updated changed in the source. */
    int UPDATE_ROW_CONFLICT = 0;

    /** The row that was to be deleted changed in the source. */
    int DELETE_ROW_CONFLICT = 1;

    /** The row that was to be inserted clashes with one that is already there. */
    int INSERT_ROW_CONFLICT = 2;

    /** This row had no conflict. */
    int NO_ROW_CONFLICT = 3;

    /**
     * What kind of conflict the current row has.
     *
     * @return one of the four constants
     */
    int getStatus();

    /**
     * The value there is <strong>in the source</strong> for that column.
     *
     * @param index the column, from 1
     * @return the source's value
     * @throws SQLException if the index is not valid or there is no current row
     */
    Object getConflictValue(int index) throws SQLException;

    /**
     * The value there is <strong>in the source</strong> for that column.
     *
     * @param columnName the name of the column
     * @return the source's value
     * @throws SQLException if the name does not exist or there is no current row
     */
    Object getConflictValue(String columnName) throws SQLException;

    /**
     * Sets the value the conflict of that column is resolved with.
     *
     * @param index the column, from 1
     * @param obj the value that is going to stay
     * @throws SQLException if the index is not valid or there is no current row
     */
    void setResolvedValue(int index, Object obj) throws SQLException;

    /**
     * Sets the value the conflict of that column is resolved with.
     *
     * @param columnName the name of the column
     * @param obj the value that is going to stay
     * @throws SQLException if the name does not exist or there is no current row
     */
    void setResolvedValue(String columnName, Object obj) throws SQLException;

    /**
     * Moves forward to the next row in conflict.
     *
     * @return {@code true} if there was another
     * @throws SQLException if it could not move forward
     */
    boolean nextConflict() throws SQLException;

    /**
     * Moves back to the previous row in conflict.
     *
     * @return {@code true} if there was another
     * @throws SQLException if it could not move back
     */
    boolean previousConflict() throws SQLException;
}
