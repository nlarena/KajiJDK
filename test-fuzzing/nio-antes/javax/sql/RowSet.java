package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSet -- a {@link java.sql.ResultSet} that can look after itself.
 *
 * <p>An ordinary `ResultSet` dies with its connection; a `RowSet` keeps **how** to get its data
 * --the URL, the query, the parameters-- and can go and fetch it again with {@link #execute}. That
 * is what makes it serializable and transportable: it can be sent to another machine, edited
 * without a connection and synchronized later.
 *
 * <p>Being a JavaBean --properties with `get`/`set` and listeners-- is no accident: it was designed
 * so that a visual tool could configure it without writing code.
 *
 * <p><strong>Declared subset.</strong> The configuration, the execution, the listeners and the
 * parameter `setXxx`s of the types this library has are here. The families of SQL-specific types
 * are left out, for the same reason as in {@link java.sql.ResultSet}.
 */
public interface RowSet extends java.sql.ResultSet {

    // ---- where the data comes from --------------------------------------------------------------
    //
    // Two alternative, mutually exclusive routes: a JDBC URL, or the name of a `DataSource` in a
    // directory. The second is what allows moving the application to another database without
    // touching it.

    String getUrl() throws java.sql.SQLException;

    void setUrl(String url) throws java.sql.SQLException;

    String getDataSourceName();

    void setDataSourceName(String name) throws java.sql.SQLException;

    String getUsername();

    void setUsername(String name) throws java.sql.SQLException;

    String getPassword();

    void setPassword(String password) throws java.sql.SQLException;

    /** The query that fills this set. */
    String getCommand();

    void setCommand(String cmd) throws java.sql.SQLException;

    /** Goes and fetches the data. */
    void execute() throws java.sql.SQLException;

    // ---- configuration --------------------------------------------------------------------------

    boolean isReadOnly();

    void setReadOnly(boolean value) throws java.sql.SQLException;

    int getTransactionIsolation();

    void setTransactionIsolation(int level) throws java.sql.SQLException;

    int getMaxFieldSize() throws java.sql.SQLException;

    void setMaxFieldSize(int max) throws java.sql.SQLException;

    int getMaxRows() throws java.sql.SQLException;

    void setMaxRows(int max) throws java.sql.SQLException;

    int getQueryTimeout() throws java.sql.SQLException;

    void setQueryTimeout(int seconds) throws java.sql.SQLException;

    boolean getEscapeProcessing() throws java.sql.SQLException;

    void setEscapeProcessing(boolean enable) throws java.sql.SQLException;

    void setType(int type) throws java.sql.SQLException;

    void setConcurrency(int concurrency) throws java.sql.SQLException;

    java.util.Map<String, Class<?>> getTypeMap() throws java.sql.SQLException;

    void setTypeMap(java.util.Map<String, Class<?>> map) throws java.sql.SQLException;

    // ---- listeners ------------------------------------------------------------------------------

    void addRowSetListener(RowSetListener listener);

    void removeRowSetListener(RowSetListener listener);

    // ---- query parameters -----------------------------------------------------------------------

    void setNull(int parameterIndex, int sqlType) throws java.sql.SQLException;

    void setBoolean(int parameterIndex, boolean x) throws java.sql.SQLException;

    void setByte(int parameterIndex, byte x) throws java.sql.SQLException;

    void setShort(int parameterIndex, short x) throws java.sql.SQLException;

    void setInt(int parameterIndex, int x) throws java.sql.SQLException;

    void setLong(int parameterIndex, long x) throws java.sql.SQLException;

    void setFloat(int parameterIndex, float x) throws java.sql.SQLException;

    void setDouble(int parameterIndex, double x) throws java.sql.SQLException;

    void setBigDecimal(int parameterIndex, java.math.BigDecimal x) throws java.sql.SQLException;

    void setString(int parameterIndex, String x) throws java.sql.SQLException;

    void setBytes(int parameterIndex, byte[] x) throws java.sql.SQLException;

    void setObject(int parameterIndex, Object x) throws java.sql.SQLException;

    /** Forgets the parameters that were set. */
    void clearParameters() throws java.sql.SQLException;

    // ---- the rest of the parameters -------------------------------------------------------------
    //
    // The same as `PreparedStatement`'s and also **by name**: a `RowSet` is configured from outside
    // --from a file, from a visual tool-- and there a position number tells nobody anything. That
    // is why each `setXxx(int, ...)` has its twin `setXxx(String, ...)`, which the prepared
    // statement does not need.

    void setArray(int parameterIndex, java.sql.Array x) throws java.sql.SQLException;

    void setAsciiStream(int parameterIndex, java.io.InputStream x) throws java.sql.SQLException;

    void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws java.sql.SQLException;

    void setAsciiStream(java.lang.String parameterName, java.io.InputStream x) throws java.sql.SQLException;

    void setAsciiStream(java.lang.String parameterName, java.io.InputStream x, int length) throws java.sql.SQLException;

    void setBigDecimal(java.lang.String parameterName, java.math.BigDecimal x) throws java.sql.SQLException;

    void setBinaryStream(int parameterIndex, java.io.InputStream x) throws java.sql.SQLException;

    void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws java.sql.SQLException;

    void setBinaryStream(java.lang.String parameterName, java.io.InputStream x) throws java.sql.SQLException;

    void setBinaryStream(java.lang.String parameterName, java.io.InputStream x, int length) throws java.sql.SQLException;

    void setBlob(int parameterIndex, java.io.InputStream x) throws java.sql.SQLException;

    void setBlob(int parameterIndex, java.io.InputStream x, long length) throws java.sql.SQLException;

    void setBlob(int parameterIndex, java.sql.Blob x) throws java.sql.SQLException;

    void setBlob(java.lang.String parameterName, java.io.InputStream x) throws java.sql.SQLException;

    void setBlob(java.lang.String parameterName, java.io.InputStream x, long length) throws java.sql.SQLException;

    void setBlob(java.lang.String parameterName, java.sql.Blob x) throws java.sql.SQLException;

    void setBoolean(java.lang.String parameterName, boolean x) throws java.sql.SQLException;

    void setByte(java.lang.String parameterName, byte x) throws java.sql.SQLException;

    void setBytes(java.lang.String parameterName, byte[] x) throws java.sql.SQLException;

    void setCharacterStream(int parameterIndex, java.io.Reader x) throws java.sql.SQLException;

    void setCharacterStream(int parameterIndex, java.io.Reader x, int length) throws java.sql.SQLException;

    void setCharacterStream(java.lang.String parameterName, java.io.Reader x) throws java.sql.SQLException;

    void setCharacterStream(java.lang.String parameterName, java.io.Reader x, int length) throws java.sql.SQLException;

    void setClob(int parameterIndex, java.io.Reader x) throws java.sql.SQLException;

    void setClob(int parameterIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void setClob(int parameterIndex, java.sql.Clob x) throws java.sql.SQLException;

    void setClob(java.lang.String parameterName, java.io.Reader x) throws java.sql.SQLException;

    void setClob(java.lang.String parameterName, java.io.Reader x, long length) throws java.sql.SQLException;

    void setClob(java.lang.String parameterName, java.sql.Clob x) throws java.sql.SQLException;

    void setDate(int parameterIndex, java.sql.Date x) throws java.sql.SQLException;

    void setDate(int parameterIndex, java.sql.Date x, java.util.Calendar cal) throws java.sql.SQLException;

    void setDate(java.lang.String parameterName, java.sql.Date x) throws java.sql.SQLException;

    void setDate(java.lang.String parameterName, java.sql.Date x, java.util.Calendar cal) throws java.sql.SQLException;

    void setDouble(java.lang.String parameterName, double x) throws java.sql.SQLException;

    void setFloat(java.lang.String parameterName, float x) throws java.sql.SQLException;

    void setInt(java.lang.String parameterName, int x) throws java.sql.SQLException;

    void setLong(java.lang.String parameterName, long x) throws java.sql.SQLException;

    void setNCharacterStream(int parameterIndex, java.io.Reader x) throws java.sql.SQLException;

    void setNCharacterStream(int parameterIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void setNCharacterStream(java.lang.String parameterName, java.io.Reader x) throws java.sql.SQLException;

    void setNCharacterStream(java.lang.String parameterName, java.io.Reader x, long length) throws java.sql.SQLException;

    void setNClob(int parameterIndex, java.io.Reader x) throws java.sql.SQLException;

    void setNClob(int parameterIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void setNClob(int parameterIndex, java.sql.NClob x) throws java.sql.SQLException;

    void setNClob(java.lang.String parameterName, java.io.Reader x) throws java.sql.SQLException;

    void setNClob(java.lang.String parameterName, java.io.Reader x, long length) throws java.sql.SQLException;

    void setNClob(java.lang.String parameterName, java.sql.NClob x) throws java.sql.SQLException;

    void setNString(int parameterIndex, java.lang.String x) throws java.sql.SQLException;

    void setNString(java.lang.String parameterName, java.lang.String x) throws java.sql.SQLException;

    void setNull(int parameterIndex, int sqlType, java.lang.String typeName) throws java.sql.SQLException;

    void setNull(java.lang.String parameterIndex, int sqlType) throws java.sql.SQLException;

    void setNull(java.lang.String parameterIndex, int sqlType, java.lang.String typeName) throws java.sql.SQLException;

    void setObject(int parameterIndex, java.lang.Object x, int targetSqlType) throws java.sql.SQLException;

    void setObject(int parameterIndex, java.lang.Object x, int targetSqlType, int scaleOrLength) throws java.sql.SQLException;

    void setObject(java.lang.String parameterIndex, java.lang.Object x) throws java.sql.SQLException;

    void setObject(java.lang.String parameterIndex, java.lang.Object x, int targetSqlType) throws java.sql.SQLException;

    void setObject(java.lang.String parameterIndex, java.lang.Object x, int targetSqlType, int scaleOrLength) throws java.sql.SQLException;

    void setRef(int parameterIndex, java.sql.Ref x) throws java.sql.SQLException;

    void setRowId(int parameterIndex, java.sql.RowId x) throws java.sql.SQLException;

    void setRowId(java.lang.String parameterName, java.sql.RowId x) throws java.sql.SQLException;

    void setSQLXML(int parameterIndex, java.sql.SQLXML x) throws java.sql.SQLException;

    void setSQLXML(java.lang.String parameterName, java.sql.SQLXML x) throws java.sql.SQLException;

    void setShort(java.lang.String parameterName, short x) throws java.sql.SQLException;

    void setString(java.lang.String parameterName, java.lang.String x) throws java.sql.SQLException;

    void setTime(int parameterIndex, java.sql.Time x) throws java.sql.SQLException;

    void setTime(int parameterIndex, java.sql.Time x, java.util.Calendar cal) throws java.sql.SQLException;

    void setTime(java.lang.String parameterName, java.sql.Time x) throws java.sql.SQLException;

    void setTime(java.lang.String parameterName, java.sql.Time x, java.util.Calendar cal) throws java.sql.SQLException;

    void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws java.sql.SQLException;

    void setTimestamp(int parameterIndex, java.sql.Timestamp x, java.util.Calendar cal) throws java.sql.SQLException;

    void setTimestamp(java.lang.String parameterName, java.sql.Timestamp x) throws java.sql.SQLException;

    void setTimestamp(java.lang.String parameterName, java.sql.Timestamp x, java.util.Calendar cal) throws java.sql.SQLException;

    void setURL(int parameterIndex, java.net.URL x) throws java.sql.SQLException;
}
