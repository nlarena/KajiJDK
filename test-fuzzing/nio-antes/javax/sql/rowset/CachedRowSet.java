package javax.sql.rowset;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Collection;

import javax.sql.RowSet;
import javax.sql.RowSetEvent;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.spi.SyncProvider;
import javax.sql.rowset.spi.SyncProviderException;

/**
 * A set of rows that lives <strong>disconnected</strong> from the database.
 *
 * <h2>What it gains by disconnecting</h2>
 *
 * <p>That it does not hold a connection while somebody looks at it. A connection is a scarce and
 * expensive resource; an ordinary {@code ResultSet} holds it from the moment it opens until it
 * closes, and in an application with thousands of users that does not scale. This set fills up,
 * lets go of the connection, and afterwards can be walked, modified and even serialized and sent
 * over the network.
 *
 * <p>It is also {@code Serializable} and scrollable in both directions, things a forward-only
 * {@code ResultSet} cannot offer precisely because it is tied to the server's cursor.
 *
 * <h2>What it costs: conflicts</h2>
 *
 * <p>Time passes between reading and writing, and in that time somebody else may have touched the
 * same rows. That is why {@link #acceptChanges} throws {@link SyncProviderException} and not an
 * ordinary {@code SQLException}: inside comes the {@link javax.sql.rowset.spi.SyncResolver} with
 * the rows that clashed, to resolve them one by one instead of losing the whole batch.
 *
 * <p>That is the core of the design: disconnecting does not eliminate the concurrency problem, it
 * <strong>moves</strong> it from the server to the client and makes it explicit.
 *
 * <h2>The original rows</h2>
 *
 * <p>The set keeps two versions of each modified row: the one that was loaded and the one the user
 * left. The original is what allows conflicts to be detected —there is something to compare
 * against— and also what makes {@link #undoUpdate} and {@link #restoreOriginal} possible. Without
 * it, undoing would mean querying again.
 *
 * <h2>Paging</h2>
 *
 * <p>{@link #setPageSize} and {@link #nextPage} serve for a result that does not fit in memory: it
 * is fetched so many rows at a time. The trade-off is that the pages are read at different moments,
 * so two pages of the same walk may not be consistent with each other.
 *
 * @since 1.5
 */
public interface CachedRowSet extends RowSet, Joinable {

    /**
     * Whether {@link #acceptChanges()} commits the transaction on its own.
     *
     * <p>It is {@code true} and it is a constant, not a property: the behaviour is fixed by the
     * specification.
     */
    boolean COMMIT_ON_ACCEPT_CHANGES = true;

    /**
     * Fills the set with whatever there is in an already open {@code ResultSet}.
     *
     * @param data the result to copy from
     * @throws SQLException if it could not be read
     */
    void populate(ResultSet data) throws SQLException;

    /**
     * Connects with the given connection, executes the query and disconnects.
     *
     * <p>The specification says this method also closes {@code conn} once it has populated the set.
     * (The note said the caller closes it, not this method, because it was lent.)
     *
     * @param conn the connection to use
     * @throws SQLException if the query failed
     */
    void execute(Connection conn) throws SQLException;

    /**
     * Returns the changes to the source.
     *
     * @throws SyncProviderException if there were conflicts; it carries the resolver inside
     */
    void acceptChanges() throws SyncProviderException;

    /**
     * Returns the changes to the source using the given connection.
     *
     * @param con the connection to use
     * @throws SyncProviderException if there were conflicts; it carries the resolver inside
     */
    void acceptChanges(Connection con) throws SyncProviderException;

    /**
     * Discards all the changes and goes back to the content it was loaded with.
     *
     * @throws SQLException if it could not be restored
     */
    void restoreOriginal() throws SQLException;

    /**
     * Lets go of the content, leaving the set empty but with its properties.
     *
     * @throws SQLException if it could not be released
     */
    void release() throws SQLException;

    /**
     * Undoes the deletion of the current row.
     *
     * @throws SQLException if the current row was not deleted
     */
    void undoDelete() throws SQLException;

    /**
     * Undoes the insertion of the current row.
     *
     * @throws SQLException if the current row was not an insertion
     */
    void undoInsert() throws SQLException;

    /**
     * Undoes the modification of the current row.
     *
     * @throws SQLException if the current row was not modified
     */
    void undoUpdate() throws SQLException;

    /**
     * Whether that column of the current row was modified.
     *
     * @param idx the column, from 1
     * @return whether it changed
     * @throws SQLException if the index is not valid
     */
    boolean columnUpdated(int idx) throws SQLException;

    /**
     * Whether that column of the current row was modified.
     *
     * @param columnName the name of the column
     * @return whether it changed
     * @throws SQLException if the name does not exist
     */
    boolean columnUpdated(String columnName) throws SQLException;

    /**
     * The whole set as a collection of rows.
     *
     * @return the collection
     * @throws SQLException if it could not be built
     */
    Collection<?> toCollection() throws SQLException;

    /**
     * A whole column as a collection of values.
     *
     * @param column the column, from 1
     * @return the collection
     * @throws SQLException if the index is not valid
     */
    Collection<?> toCollection(int column) throws SQLException;

    /**
     * A whole column as a collection of values.
     *
     * @param column the name of the column
     * @return the collection
     * @throws SQLException if the name does not exist
     */
    Collection<?> toCollection(String column) throws SQLException;

    /**
     * The provider that synchronizes this set with its source.
     *
     * @return the provider
     * @throws SQLException if it could not be obtained
     */
    SyncProvider getSyncProvider() throws SQLException;

    /**
     * Changes the synchronization provider.
     *
     * @param provider the provider's identifier
     * @throws SQLException if it is not registered or could not be instantiated
     */
    void setSyncProvider(String provider) throws SQLException;

    /**
     * How many rows there are.
     *
     * @return the count
     */
    int size();

    /**
     * Sets the metadata of the columns.
     *
     * <p>It is needed when the set is filled by hand and not from a {@code ResultSet}: without
     * metadata there are no column names nor types, and almost nothing of the rest of the interface
     * works.
     *
     * @param md the metadata
     * @throws SQLException if it could not be set
     */
    void setMetaData(RowSetMetaData md) throws SQLException;

    /**
     * The original content of the whole set, as a {@code ResultSet}.
     *
     * @return the original content
     * @throws SQLException if it could not be built
     */
    ResultSet getOriginal() throws SQLException;

    /**
     * The original content of the current row.
     *
     * @return the original row
     * @throws SQLException if there is no current row
     */
    ResultSet getOriginalRow() throws SQLException;

    /**
     * Declares that the current row becomes the original.
     *
     * <p>It is what is done after synchronizing successfully: what was just written is now what
     * there is in the source, so that is what has to be compared against next time.
     *
     * @throws SQLException if there is no current row
     */
    void setOriginalRow() throws SQLException;

    /**
     * The table the changes are written against.
     *
     * @return the name of the table
     * @throws SQLException if it could not be obtained
     */
    String getTableName() throws SQLException;

    /**
     * Sets the table to write against.
     *
     * <p>It is needed when the query touched several tables: the set cannot guess which one to
     * write to, and without this {@link #acceptChanges} has no destination.
     *
     * @param tabName the name of the table
     * @throws SQLException if the name is invalid
     */
    void setTableName(String tabName) throws SQLException;

    /**
     * The columns that identify a row.
     *
     * @return the indices, from 1
     * @throws SQLException if they could not be obtained
     */
    int[] getKeyColumns() throws SQLException;

    /**
     * Sets the columns that identify a row.
     *
     * <p>It is what the writer uses to build the {@code WHERE} when updating. Without keys it would
     * have to compare all the columns, which is slower and fails with the ones that cannot be
     * compared.
     *
     * @param keys the indices, from 1
     * @throws SQLException if some index is not valid
     */
    void setKeyColumns(int[] keys) throws SQLException;

    /**
     * Another set that shares this one's data.
     *
     * <p>They share the rows and have <strong>different cursors</strong>: two independent walks
     * over the same data, without copying it. Modifying through one is seen through the other.
     *
     * @return the shared set
     * @throws SQLException if it could not be created
     */
    RowSet createShared() throws SQLException;

    /**
     * An independent copy, with data and state.
     *
     * @return the copy
     * @throws SQLException if it could not be copied
     */
    CachedRowSet createCopy() throws SQLException;

    /**
     * A copy with the columns but without the rows.
     *
     * @return the empty copy
     * @throws SQLException if it could not be copied
     */
    CachedRowSet createCopySchema() throws SQLException;

    /**
     * A deep copy of the data, independent of this set.
     *
     * <p>The note said the constraints left behind are the read-only mark, the cursor type and the
     * isolation level; the specification says only that it is a deep copy of the data, independent
     * of this one.
     *
     * @return the copy without constraints
     * @throws SQLException if it could not be copied
     */
    CachedRowSet createCopyNoConstraints() throws SQLException;

    /**
     * The accumulated warnings.
     *
     * @return the first of the chain, or {@code null}
     * @throws SQLException if they could not be obtained
     */
    RowSetWarning getRowSetWarnings() throws SQLException;

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
     * <p>Deleted rows do not disappear until synchronizing —it has to be remembered that they were
     * deleted in order to write it later—, so the question is only whether the walk goes through
     * them.
     *
     * @param b whether to show them
     * @throws SQLException if it could not be changed
     */
    void setShowDeleted(boolean b) throws SQLException;

    /**
     * Commits the transaction of the underlying connection.
     *
     * @throws SQLException if it could not be committed
     */
    void commit() throws SQLException;

    /**
     * Rolls back the transaction of the underlying connection.
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

    /**
     * Notice that the set was filled, with the row where the page started.
     *
     * @param event the event
     * @param numRows the starting row of the page
     * @throws SQLException if it could not be processed
     */
    void rowSetPopulated(RowSetEvent event, int numRows) throws SQLException;

    /**
     * Fills the set from a row onwards.
     *
     * @param startRow the row of the result to start from, from 1
     * @param rs the result to copy from
     * @throws SQLException if it could not be read
     */
    void populate(ResultSet rs, int startRow) throws SQLException;

    /**
     * How many rows each page brings.
     *
     * @param size the size; zero turns paging off
     * @throws SQLException if the size is negative or exceeds the maximum number of rows
     */
    void setPageSize(int size) throws SQLException;

    /**
     * The page size.
     *
     * @return the size
     */
    int getPageSize();

    /**
     * Fetches the next page.
     *
     * @return {@code true} if there was another
     * @throws SQLException if it could not be read
     */
    boolean nextPage() throws SQLException;

    /**
     * Fetches the previous page.
     *
     * @return {@code true} if there was another
     * @throws SQLException if it could not be read
     */
    boolean previousPage() throws SQLException;
}
