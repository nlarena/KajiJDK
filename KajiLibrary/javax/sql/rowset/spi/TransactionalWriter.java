package javax.sql.rowset.spi;

import java.sql.SQLException;
import java.sql.Savepoint;

import javax.sql.RowSetWriter;

/**
 * A writer that also knows how to commit and roll back.
 *
 * <h2>Why it is not in {@link RowSetWriter}</h2>
 *
 * <p>Because not every source has transactions. A writer against an XML file can write and cannot
 * undo; forcing it to declare {@code rollback} would force it to have a method that lies or always
 * fails.
 *
 * <p>By separating the capability into its own interface, a {@code CachedRowSet} asks with
 * {@code instanceof} whether the writer it got can, instead of trying it and seeing what happens.
 *
 * <h2>Savepoints</h2>
 *
 * <p>{@link #rollback(Savepoint)} rolls back to a mark instead of rolling back everything. It
 * serves when a batch of rows is written together and a single one fails: it goes back to before
 * that row and the rest of the batch is kept.
 *
 * @since 1.5
 */
public interface TransactionalWriter extends RowSetWriter {

    /**
     * Commits what was written.
     *
     * @throws SQLException if it could not be committed
     */
    void commit() throws SQLException;

    /**
     * Rolls back everything written since the last commit.
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
