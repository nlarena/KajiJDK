package javax.sql.rowset;

import java.sql.SQLException;
import java.util.Collection;

import javax.sql.RowSet;

/**
 * A {@code RowSet} that is the join of several others, done <strong>without a database</strong>.
 *
 * <h2>What joining on the client is for</h2>
 *
 * <p>To bring together data that is not in the same database. One set comes from one system,
 * another from an XML file, a third was built by hand: no SQL {@code JOIN} can touch all three, and
 * this can.
 *
 * <p>It also serves when the data has already been fetched: joining in memory avoids a second
 * query.
 *
 * <h2>Why the sets have to be {@link Joinable}</h2>
 *
 * <p>Because the join needs to know <strong>by which column</strong>. A {@code Joinable} declares
 * its match column, and {@link #addRowSet(Joinable)} uses that declaration; the other overloads set
 * it on the spot, for a set that did not come with it.
 *
 * <h2>The join types and the method to call first</h2>
 *
 * <p>The five {@code supports*} answer which ones are implemented, and it is not a formality: the
 * specification only <strong>requires</strong> {@link #INNER_JOIN}. The other four are optional,
 * and calling {@link #setJoinType} with one the implementation does not have fails. Asking first is
 * the intended use.
 *
 * <h2>The join cannot be undone piecemeal</h2>
 *
 * <p>There is no {@code removeRowSet}. Once a set has been added, the only thing left is to build
 * another {@code JoinRowSet}. It is a real limitation of the interface and it is worth knowing
 * before designing around it.
 *
 * @since 1.5
 */
public interface JoinRowSet extends WebRowSet {

    /** Cartesian product: each row of one side with each row of the other. */
    int CROSS_JOIN = 0;

    /** Only the rows that match on both sides. It is the only one the specification requires. */
    int INNER_JOIN = 1;

    /** All the ones of the first, with nulls where the second has no match. */
    int LEFT_OUTER_JOIN = 2;

    /** All the ones of the second, with nulls where the first has no match. */
    int RIGHT_OUTER_JOIN = 3;

    /** All the ones of both sides, with nulls where the match is missing. */
    int FULL_JOIN = 4;

    /**
     * Adds a set that already has its match column declared.
     *
     * @param rowset the set
     * @throws SQLException if it declared no match column
     */
    void addRowSet(Joinable rowset) throws SQLException;

    /**
     * Adds a set and sets its match column.
     *
     * @param rowset the set
     * @param columnIdx the column, from 1
     * @throws SQLException if the index is not valid
     */
    void addRowSet(RowSet rowset, int columnIdx) throws SQLException;

    /**
     * Adds a set and sets its match column by name.
     *
     * @param rowset the set
     * @param columnName the name of the column
     * @throws SQLException if the name does not exist
     */
    void addRowSet(RowSet rowset, String columnName) throws SQLException;

    /**
     * Adds several sets at once, with their columns.
     *
     * <p>The two arrays correspond position by position.
     *
     * @param rowset the sets
     * @param columnIdx the columns, one per set
     * @throws SQLException if the arrays do not have the same length or some index is not valid
     */
    void addRowSet(RowSet[] rowset, int[] columnIdx) throws SQLException;

    /**
     * Adds several sets at once, with their columns by name.
     *
     * @param rowset the sets
     * @param columnName the names, one per set
     * @throws SQLException if the arrays do not have the same length or some name does not exist
     */
    void addRowSet(RowSet[] rowset, String[] columnName) throws SQLException;

    /**
     * The sets that take part in the join.
     *
     * @return the collection
     * @throws SQLException if it could not be obtained
     */
    Collection<?> getRowSets() throws SQLException;

    /**
     * The names of the sets that take part.
     *
     * @return the names
     * @throws SQLException if they could not be obtained
     */
    String[] getRowSetNames() throws SQLException;

    /**
     * The result of the join as an ordinary {@link CachedRowSet}.
     *
     * <p>It is the way of getting the result out of here: what is obtained no longer remembers
     * which sets it came from and behaves like any other disconnected set.
     *
     * @return the result
     * @throws SQLException if it could not be built
     */
    CachedRowSet toCachedRowSet() throws SQLException;

    /**
     * Whether this implementation supports the cartesian product.
     *
     * @return whether it supports it
     */
    boolean supportsCrossJoin();

    /**
     * Whether this implementation supports the inner join.
     *
     * @return whether it supports it; the specification requires that it does
     */
    boolean supportsInnerJoin();

    /**
     * Whether this implementation supports the left outer join.
     *
     * @return whether it supports it
     */
    boolean supportsLeftOuterJoin();

    /**
     * Whether this implementation supports the right outer join.
     *
     * @return whether it supports it
     */
    boolean supportsRightOuterJoin();

    /**
     * Whether this implementation supports the full outer join.
     *
     * @return whether it supports it
     */
    boolean supportsFullJoin();

    /**
     * Sets the join type.
     *
     * @param joinType one of the five constants
     * @throws SQLException if this implementation does not support that type
     */
    void setJoinType(int joinType) throws SQLException;

    /**
     * A description of the {@code WHERE} clause of the join.
     *
     * <p>It serves to show somebody what was done. The specification allows an SQL-like description
     * or a plain textual one, so it is not necessarily something another database can execute. (The
     * note said it is the SQL {@code WHERE} clause itself, good for taking the join to a database
     * that can run it better.)
     *
     * @return the clause
     * @throws SQLException if it could not be built
     */
    String getWhereClause() throws SQLException;

    /**
     * The join type in use.
     *
     * @return one of the five constants
     * @throws SQLException if it could not be queried
     */
    int getJoinType() throws SQLException;
}
