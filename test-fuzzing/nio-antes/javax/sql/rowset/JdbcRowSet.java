package javax.sql.rowset;

import java.sql.SQLException;
import java.sql.Savepoint;

import javax.sql.RowSet;

/**
 * A <strong>connected</strong> {@code RowSet}: a thin wrapper over a {@code ResultSet}.
 *
 * <h2>What it adds if {@code ResultSet} already exists</h2>
 *
 * <p>Two things. It is a JavaBeans-style component —it has properties that are set and then it is
 * executed— and it emits events, so a graphical interface can hook onto it. And it is scrollable
 * and updatable even if the underlying driver is not by itself.
 *
 * <p>What it does <strong>not</strong> add is disconnection: it keeps the connection open the whole
 * time, like a {@code ResultSet}. To let go of it there is {@link CachedRowSet}.
 *
 * <h2>When this one is preferable to a {@link CachedRowSet}</h2>
 *
 * <p>When the data has to be fresh and the set is large. Being connected, what is read is what
 * there is now, and there are no conflicts to resolve because there is no window between reading
 * and writing. The price is the held connection.
 *
 * @since 1.5
 */
public interface JdbcRowSet extends RowSet, Joinable {

    /**
     * Whether deleted rows are still seen when walking.
     *
     * @return whether they are shown
     * @throws SQLException if it could not be queried
     */
    boolean getShowDeleted() throws SQLException;

    /**
     * Shows or hides the deleted rows.
     *
     * @param b whether to show them
     * @throws SQLException if it could not be changed
     */
    void setShowDeleted(boolean b) throws SQLException;

    /**
     * The accumulated warnings.
     *
     * @return the first of the chain, or {@code null}
     * @throws SQLException if they could not be obtained
     */
    RowSetWarning getRowSetWarnings() throws SQLException;

    /**
     * Commits the transaction.
     *
     * @throws SQLException if it could not be committed
     */
    void commit() throws SQLException;

    /**
     * Whether the connection commits each statement by itself.
     *
     * @return whether it is in auto-commit
     * @throws SQLException if it could not be queried
     */
    boolean getAutoCommit() throws SQLException;

    /**
     * Turns auto-commit on or off.
     *
     * <p>Turning it off is what makes {@link #rollback} possible: with auto-commit on, each
     * statement was already written and there is nothing to undo.
     *
     * @param autoCommit whether to commit by itself
     * @throws SQLException if it could not be changed
     */
    void setAutoCommit(boolean autoCommit) throws SQLException;

    /**
     * Rolls back the transaction.
     *
     * @throws SQLException if it could not be rolled back
     */
    void rollback() throws SQLException;

    /**
     * Rolls back to the given savepoint.
     *
     * @param s the savepoint
     * @throws SQLException if it could not be rolled back
     */
    void rollback(Savepoint s) throws SQLException;
}
