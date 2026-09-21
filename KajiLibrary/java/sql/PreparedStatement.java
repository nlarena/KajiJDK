package java.sql;

/**
 * KajiLibrary's java.sql.PreparedStatement -- a statement with holes, and the values apart.
 *
 * <p>Two reasons to prefer it always, and the second matters more than it looks. The first is
 * performance: the database parses the statement once and reuses it with different values. The
 * second is that **no SQL injection is possible**: the value travels by a channel different from
 * the statement's text, so a value containing `'; drop table` is a value containing those
 * characters and never code. It is not that they are escaped well -- it is that they are not mixed.
 *
 * <p>The parameters are numbered **from one**, like the columns.
 *
 * <p><strong>The interface is complete.</strong> This note used to say the setters taking SQL types
 * of their own (`setBlob`, `setArray`, `setSQLXML`, the streams with a length) were left out; all
 * of them are declared, and the section further down already describes the whole family.
 */
public interface PreparedStatement extends Statement {

    /** It runs the query with the parameters currently set. */
    ResultSet executeQuery() throws SQLException;

    /** It runs the modification and returns how many rows it touched. */
    int executeUpdate() throws SQLException;

    long executeLargeUpdate() throws SQLException;

    boolean execute() throws SQLException;

    /** It adds the current parameters to the batch. */
    void addBatch() throws SQLException;

    /** It forgets the parameters that were set. */
    void clearParameters() throws SQLException;

    /** What columns it would return, **without running it**. */
    ResultSetMetaData getMetaData() throws SQLException;

    // ---- the parameters -------------------------------------------------------------------------

    /**
     * It sets null.
     *
     * <p>It asks for the type because a null has one too: the database needs to know which column
     * the null is for in order to choose the plan, and cannot deduce it from a value that is not
     * there.
     */
    void setNull(int parameterIndex, int sqlType) throws SQLException;

    void setBoolean(int parameterIndex, boolean x) throws SQLException;

    void setByte(int parameterIndex, byte x) throws SQLException;

    void setShort(int parameterIndex, short x) throws SQLException;

    void setInt(int parameterIndex, int x) throws SQLException;

    void setLong(int parameterIndex, long x) throws SQLException;

    void setFloat(int parameterIndex, float x) throws SQLException;

    void setDouble(int parameterIndex, double x) throws SQLException;

    void setBigDecimal(int parameterIndex, java.math.BigDecimal x) throws SQLException;

    void setString(int parameterIndex, String x) throws SQLException;

    void setBytes(int parameterIndex, byte[] x) throws SQLException;

    void setObject(int parameterIndex, Object x) throws SQLException;

    void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException;

    // ---- the rest of the parameters -------------------------------------------------------------
    //
    // The whole family, and it is worth seeing why it is so large. There are three axes that
    // multiply: the value's **type**, whether the datum or a pointer to it is passed (`setBlob(int,
    // Blob)` against `setBlob(int, InputStream)`), and whether its size is stated. The variants
    // with a length exist because a driver that knows the size up front can reserve it in one go
    // instead of growing; the ones that do not ask for it came later, when it became clear the
    // caller almost never knows it.
    //
    // The `setN*` are the national character set version, the same distinction that separates
    // `NClob` from `Clob`.
    //
    // And `setUnicodeStream` has been deprecated since 1999: it took the text in a JDBC-specific
    // UTF-16 that was never properly defined. It is declared because the signature is the contract,
    // not because it should be used.

    java.sql.ParameterMetaData getParameterMetaData() throws java.sql.SQLException;

    void setArray(int parameterIndex, java.sql.Array x) throws java.sql.SQLException;

    void setAsciiStream(int parameterIndex, java.io.InputStream x) throws java.sql.SQLException;

    void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws java.sql.SQLException;

    void setAsciiStream(int parameterIndex, java.io.InputStream x, long length) throws java.sql.SQLException;

    void setBinaryStream(int parameterIndex, java.io.InputStream x) throws java.sql.SQLException;

    void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws java.sql.SQLException;

    void setBinaryStream(int parameterIndex, java.io.InputStream x, long length) throws java.sql.SQLException;

    void setBlob(int parameterIndex, java.io.InputStream x) throws java.sql.SQLException;

    void setBlob(int parameterIndex, java.io.InputStream x, long length) throws java.sql.SQLException;

    void setBlob(int parameterIndex, java.sql.Blob x) throws java.sql.SQLException;

    void setCharacterStream(int parameterIndex, java.io.Reader x) throws java.sql.SQLException;

    void setCharacterStream(int parameterIndex, java.io.Reader x, int length) throws java.sql.SQLException;

    void setCharacterStream(int parameterIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void setClob(int parameterIndex, java.io.Reader x) throws java.sql.SQLException;

    void setClob(int parameterIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void setClob(int parameterIndex, java.sql.Clob x) throws java.sql.SQLException;

    void setDate(int parameterIndex, java.sql.Date x) throws java.sql.SQLException;

    void setDate(int parameterIndex, java.sql.Date x, java.util.Calendar cal) throws java.sql.SQLException;

    void setNCharacterStream(int parameterIndex, java.io.Reader x) throws java.sql.SQLException;

    void setNCharacterStream(int parameterIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void setNClob(int parameterIndex, java.io.Reader x) throws java.sql.SQLException;

    void setNClob(int parameterIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void setNClob(int parameterIndex, java.sql.NClob x) throws java.sql.SQLException;

    void setNString(int parameterIndex, java.lang.String x) throws java.sql.SQLException;

    void setNull(int parameterIndex, int sqlType, java.lang.String typeName) throws java.sql.SQLException;

    void setObject(int parameterIndex, java.lang.Object x, int targetSqlType, int scaleOrLength) throws java.sql.SQLException;

    default void setObject(int parameterIndex, java.lang.Object x, java.sql.SQLType targetSqlType) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setObject not implemented");
    }

    default void setObject(int parameterIndex, java.lang.Object x, java.sql.SQLType targetSqlType, int scaleOrLength) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setObject not implemented");
    }

    void setRef(int parameterIndex, java.sql.Ref x) throws java.sql.SQLException;

    void setRowId(int parameterIndex, java.sql.RowId x) throws java.sql.SQLException;

    void setSQLXML(int parameterIndex, java.sql.SQLXML x) throws java.sql.SQLException;

    void setTime(int parameterIndex, java.sql.Time x) throws java.sql.SQLException;

    void setTime(int parameterIndex, java.sql.Time x, java.util.Calendar cal) throws java.sql.SQLException;

    void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws java.sql.SQLException;

    void setTimestamp(int parameterIndex, java.sql.Timestamp x, java.util.Calendar cal) throws java.sql.SQLException;

    void setURL(int parameterIndex, java.net.URL x) throws java.sql.SQLException;

    void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws java.sql.SQLException;
}
