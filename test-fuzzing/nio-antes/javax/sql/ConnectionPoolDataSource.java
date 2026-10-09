package javax.sql;

/**
 * KajiLibrary's javax.sql.ConnectionPoolDataSource -- where the **physical** connections come from.
 *
 * <p>The driver implements it; the pool uses it, not the application. The application sees a
 * {@link DataSource} that goes looking here underneath. That division is what allows the pool to be
 * written by someone other than the driver.
 */
public interface ConnectionPoolDataSource extends CommonDataSource {

    /** A physical connection, with the configured credentials. */
    PooledConnection getPooledConnection() throws java.sql.SQLException;

    /** A physical connection with those credentials. */
    PooledConnection getPooledConnection(String user, String password)
            throws java.sql.SQLException;

    /**
     * A builder, to ask for one with more data than user and password.
     *
     * <p>The JDK's default throws {@code SQLFeatureNotSupportedException}; this one throws
     * {@code UnsupportedOperationException}, which is unchecked and is not a {@code SQLException}.
     */
    default PooledConnectionBuilder createPooledConnectionBuilder() throws java.sql.SQLException {
        throw new UnsupportedOperationException("createPooledConnectionBuilder not implemented");
    }
}
