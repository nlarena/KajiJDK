package java.sql;

/**
 * KajiLibrary's java.sql.Connection -- a session with a database.
 *
 * <p><strong>The interface is complete.</strong> This note used to say the members that **make**
 * other JDBC objects --`createStatement`, `prepareStatement`, `getMetaData`, `createBlob`-- were
 * absent because each drags in an interface of hundreds of members that only makes sense with a
 * driver behind it. Those interfaces were written, and all four are declared below; the "making
 * statements" section further down already describes them.
 *
 * <p>Bringing the interface without a driver is honest precisely because it is an **interface**: a
 * contract does not promise anybody honours it. What could not be done is giving an implementation
 * that pretends to connect.
 *
 * <p>It is `AutoCloseable`, which is why a connection is almost always written inside a
 * try-with-resources: a connection one forgets to close does not show until the pool runs out.
 */
public interface Connection extends Wrapper, AutoCloseable {

    // ---- isolation levels -----------------------------------------------------------------------
    //
    // The four are ordered from least to most strict, and each step **removes** one phenomenon: the
    // first allows reading what another transaction has not committed; the second forbids that but
    // allows the same `select` to give two different values; the third forbids that but allows new
    // rows to appear; the fourth allows nothing. Stricter is more correct and slower, always.

    /** With no transactions. */
    int TRANSACTION_NONE = 0;

    /** It allows reading changes another transaction has not committed. */
    int TRANSACTION_READ_UNCOMMITTED = 1;

    /** It reads only what is committed; the same `select` can give different results. */
    int TRANSACTION_READ_COMMITTED = 2;

    /** The same `select` gives the same; new rows can appear. */
    int TRANSACTION_REPEATABLE_READ = 4;

    /** As if the transactions ran one at a time. */
    int TRANSACTION_SERIALIZABLE = 8;

    // ---- transaction ----------------------------------------------------------------------------

    /**
     * Whether each statement commits itself.
     *
     * <p>Turning it off is what **starts** a transaction: there is no `begin`, there is a
     * `setAutoCommit(false)`.
     */
    void setAutoCommit(boolean autoCommit) throws SQLException;

    boolean getAutoCommit() throws SQLException;

    /** It commits what has been done since the last `commit`/`rollback`. */
    void commit() throws SQLException;

    /** It discards what has been done since the last `commit`/`rollback`. */
    void rollback() throws SQLException;

    void setTransactionIsolation(int level) throws SQLException;

    int getTransactionIsolation() throws SQLException;

    /**
     * It announces that this connection will **not** write.
     *
     * <p>It is a hint so the database can optimise, not a guarantee it enforces -- and that is why
     * it can only be set outside a transaction.
     */
    void setReadOnly(boolean readOnly) throws SQLException;

    boolean isReadOnly() throws SQLException;

    // ---- life cycle -----------------------------------------------------------------------------

    /** It closes the connection and releases its resources. */
    void close() throws SQLException;

    /** Whether it has already been closed. */
    boolean isClosed() throws SQLException;

    /**
     * Whether the connection is **still alive**, waiting at most `timeout` seconds.
     *
     * <p>Different from `!isClosed()`: that one asks whether somebody closed it on this side, this
     * one asks whether there is still somebody on the other. A pool needs the second.
     *
     * @param timeout seconds to wait; zero for no limit
     */
    boolean isValid(int timeout) throws SQLException;

    /**
     * It closes the connection **from outside**, even if it is busy.
     *
     * <p>It is the way out for a hung connection: `close()` waits for the operation in progress to
     * end, and if that operation is the one that jammed, it waits forever.
     */
    void abort(java.util.concurrent.Executor executor) throws SQLException;

    /** How long an operation may take before the connection closes itself. */
    void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds)
            throws SQLException;

    int getNetworkTimeout() throws SQLException;

    // ---- context --------------------------------------------------------------------------------

    void setCatalog(String catalog) throws SQLException;

    String getCatalog() throws SQLException;

    void setSchema(String schema) throws SQLException;

    String getSchema() throws SQLException;

    /** The statement translated into this database's dialect. */
    String nativeSQL(String sql) throws SQLException;

    // ---- warnings -------------------------------------------------------------------------------

    /** The first pending warning, or `null`. */
    SQLWarning getWarnings() throws SQLException;

    /** It discards the pending warnings. */
    void clearWarnings() throws SQLException;

    // ---- making statements ----------------------------------------------------------------------
    //
    // The three families --`createStatement`, `prepareStatement`, `prepareCall`-- correspond to the
    // three ways of sending SQL, and each comes in four sizes: with no options, with the result
    // set's type and concurrency, with that plus the holdability, and --only the prepared one--
    // with the generated keys. It is combinatorics, not design: each version of the standard added
    // a parameter and could not change the earlier signatures.
    //
    // The `create*` for the large types make an **empty** {@link Blob}/{@link Clob} on the
    // database's side, to be filled before being written; without them the whole value would have
    // to be built in memory, which is exactly what those types avoid.
    //
    // The savepoints, the client information and the sharding keys complete what was missing of the
    // session.

    default boolean setShardingKeyIfValid(java.sql.ShardingKey shardingKey, int timeout) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setShardingKeyIfValid not implemented");
    }

    default boolean setShardingKeyIfValid(java.sql.ShardingKey shardingKey, java.sql.ShardingKey superShardingKey, int timeout) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setShardingKeyIfValid not implemented");
    }

    int getHoldability() throws java.sql.SQLException;

    java.lang.String getClientInfo(java.lang.String name) throws java.sql.SQLException;

    java.sql.Array createArrayOf(java.lang.String typeName, java.lang.Object[] elements) throws java.sql.SQLException;

    java.sql.Blob createBlob() throws java.sql.SQLException;

    java.sql.CallableStatement prepareCall(java.lang.String sql) throws java.sql.SQLException;

    java.sql.CallableStatement prepareCall(java.lang.String sql, int resultSetType, int resultSetConcurrency) throws java.sql.SQLException;

    java.sql.CallableStatement prepareCall(java.lang.String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws java.sql.SQLException;

    java.sql.Clob createClob() throws java.sql.SQLException;

    java.sql.DatabaseMetaData getMetaData() throws java.sql.SQLException;

    java.sql.NClob createNClob() throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql) throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql, int autoGeneratedKeys) throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql, int resultSetType, int resultSetConcurrency) throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql, int[] columnIndexes) throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql, java.lang.String[] columnNames) throws java.sql.SQLException;

    java.sql.SQLXML createSQLXML() throws java.sql.SQLException;

    java.sql.Savepoint setSavepoint() throws java.sql.SQLException;

    java.sql.Savepoint setSavepoint(java.lang.String name) throws java.sql.SQLException;

    java.sql.Statement createStatement() throws java.sql.SQLException;

    java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws java.sql.SQLException;

    java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws java.sql.SQLException;

    java.sql.Struct createStruct(java.lang.String typeName, java.lang.Object[] attributes) throws java.sql.SQLException;

    java.util.Map getTypeMap() throws java.sql.SQLException;

    java.util.Properties getClientInfo() throws java.sql.SQLException;

    default void beginRequest() throws java.sql.SQLException {
        // With no request batching: there is nothing to begin.
    }

    default void endRequest() throws java.sql.SQLException {
        // With no request batching: there is nothing to end.
    }

    void releaseSavepoint(java.sql.Savepoint savepoint) throws java.sql.SQLException;

    void rollback(java.sql.Savepoint savepoint) throws java.sql.SQLException;

    void setClientInfo(java.lang.String name, java.lang.String value) throws java.sql.SQLClientInfoException;

    void setClientInfo(java.util.Properties name) throws java.sql.SQLClientInfoException;

    void setHoldability(int holdability) throws java.sql.SQLException;

    default void setShardingKey(java.sql.ShardingKey shardingKey) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setShardingKey not implemented");
    }

    default void setShardingKey(java.sql.ShardingKey shardingKey, java.sql.ShardingKey superShardingKey) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setShardingKey not implemented");
    }

    void setTypeMap(java.util.Map map) throws java.sql.SQLException;
}
