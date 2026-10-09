package javax.sql.rowset;

import java.sql.SQLException;

/**
 * Makes the five kinds of {@code RowSet} without naming their implementations.
 *
 * <p>It is what avoids the {@code new com.sun.rowset.CachedRowSetImpl()} that appeared in code
 * before this interface existed: naming the concrete class tied the application to an
 * implementation, and changing it forced touching every creation point.
 *
 * <p>The instance is obtained with {@link RowSetProvider#newFactory()}.
 *
 * @since 1.7
 */
public interface RowSetFactory {

    /**
     * A disconnected set with a cache.
     *
     * @return the set
     * @throws SQLException if it could not be created
     */
    CachedRowSet createCachedRowSet() throws SQLException;

    /**
     * A set with a filter.
     *
     * @return the set
     * @throws SQLException if it could not be created
     */
    FilteredRowSet createFilteredRowSet() throws SQLException;

    /**
     * A connected set, a wrapper of a {@code ResultSet}.
     *
     * @return the set
     * @throws SQLException if it could not be created
     */
    JdbcRowSet createJdbcRowSet() throws SQLException;

    /**
     * A set that joins others.
     *
     * @return the set
     * @throws SQLException if it could not be created
     */
    JoinRowSet createJoinRowSet() throws SQLException;

    /**
     * A set that is serialized to XML.
     *
     * @return the set
     * @throws SQLException if it could not be created
     */
    WebRowSet createWebRowSet() throws SQLException;
}
