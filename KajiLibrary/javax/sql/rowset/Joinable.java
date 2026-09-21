package javax.sql.rowset;

import java.sql.SQLException;

/**
 * What a {@code RowSet} has to know to be able to take part in a {@link JoinRowSet}.
 *
 * <h2>What a match column is</h2>
 *
 * <p>The column by which this set joins another: the equivalent of the {@code ON} of an SQL {@code
 * JOIN}. A set of employees with the {@code department_id} column marked can be joined to one of
 * departments with {@code id} marked.
 *
 * <p>The mark lives in the set and not in the join, and that is the design decision of this
 * interface: each set declares where it lets itself be joined, and the {@link JoinRowSet} only puts
 * them together. That way the same set goes into several joins without repeating the configuration.
 *
 * <h2>Why more than one can be marked</h2>
 *
 * <p>Because a key can be composite. The versions that take arrays set several columns at once, and
 * the order matters: they correspond position by position with the other set's.
 *
 * <h2>Marking by name or by index</h2>
 *
 * <p>Both ways exist and they are not equivalent in the worst case: a set without name metadata
 * cannot resolve the name, and there the index version is the only one that works.
 *
 * @since 1.5
 */
public interface Joinable {

    /**
     * Marks a column as a match column.
     *
     * @param columnIdx the column, from 1
     * @throws SQLException if the index is not valid
     */
    void setMatchColumn(int columnIdx) throws SQLException;

    /**
     * Marks several columns, in order.
     *
     * @param columnIdxes the columns, from 1
     * @throws SQLException if some index is not valid
     */
    void setMatchColumn(int[] columnIdxes) throws SQLException;

    /**
     * Marks a column by name.
     *
     * @param columnName the name
     * @throws SQLException if the name does not exist
     */
    void setMatchColumn(String columnName) throws SQLException;

    /**
     * Marks several columns by name, in order.
     *
     * @param columnNames the names
     * @throws SQLException if some name does not exist
     */
    void setMatchColumn(String[] columnNames) throws SQLException;

    /**
     * The indices of the marked columns.
     *
     * @return the indices
     * @throws SQLException if none is marked
     */
    int[] getMatchColumnIndexes() throws SQLException;

    /**
     * The names of the marked columns.
     *
     * @return the names
     * @throws SQLException if none is marked
     */
    String[] getMatchColumnNames() throws SQLException;

    /**
     * Unmarks a column.
     *
     * @param columnIdx the column, from 1
     * @throws SQLException if that column was not marked
     */
    void unsetMatchColumn(int columnIdx) throws SQLException;

    /**
     * Unmarks several columns.
     *
     * @param columnIdxes the columns, from 1
     * @throws SQLException if some was not marked
     */
    void unsetMatchColumn(int[] columnIdxes) throws SQLException;

    /**
     * Unmarks a column by name.
     *
     * @param columnName the name
     * @throws SQLException if that column was not marked
     */
    void unsetMatchColumn(String columnName) throws SQLException;

    /**
     * Unmarks several columns by name.
     *
     * @param columnName the names
     * @throws SQLException if some was not marked
     */
    void unsetMatchColumn(String[] columnName) throws SQLException;
}
