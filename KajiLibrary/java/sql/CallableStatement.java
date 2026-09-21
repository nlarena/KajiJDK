package java.sql;

/**
 * KajiLibrary's java.sql.CallableStatement -- calls a stored procedure.
 *
 * <p>What separates it from {@link PreparedStatement} is that parameters can go **both ways**. A
 * procedure can return through a parameter, and that forces a step no other statement has:
 * {@link #registerOutParameter}, which has to be called **before** executing. The driver needs to
 * know the type of what will come back to reserve the space; it cannot deduce it from the value
 * because there is no value yet.
 *
 * <p>Hence also the `getXxx` family that inherits in spirit from {@link ResultSet} but lives here:
 * results through parameters are read from the statement, not from a set of rows.
 *
 * <p>And hence the duplication by index and by name: a procedure with twelve parameters of which
 * three are outputs is exactly where counting positions stops being viable.
 */
public interface CallableStatement extends PreparedStatement {

    // ---- registering outputs, and reading them --------------------------------------------------
    //
    // The three families are the three things a procedure call needs: registering which parameters
    // are outputs and of what type, setting the input ones **by name** --which the prepared
    // statement does not allow-- and reading the outputs after executing.
    //
    // {@link #wasNull} matters here for the same reason as in {@link java.sql.ResultSet}: a
    // `getInt` that returns zero does not tell zero from null.

    boolean getBoolean(int parameterIndex) throws java.sql.SQLException;

    boolean getBoolean(java.lang.String parameterName) throws java.sql.SQLException;

    boolean wasNull() throws java.sql.SQLException;

    byte getByte(int parameterIndex) throws java.sql.SQLException;

    byte getByte(java.lang.String parameterName) throws java.sql.SQLException;

    byte[] getBytes(int parameterIndex) throws java.sql.SQLException;

    byte[] getBytes(java.lang.String parameterName) throws java.sql.SQLException;

    double getDouble(int parameterIndex) throws java.sql.SQLException;

    double getDouble(java.lang.String parameterName) throws java.sql.SQLException;

    float getFloat(int parameterIndex) throws java.sql.SQLException;

    float getFloat(java.lang.String parameterName) throws java.sql.SQLException;

    int getInt(int parameterIndex) throws java.sql.SQLException;

    int getInt(java.lang.String parameterName) throws java.sql.SQLException;

    java.io.Reader getCharacterStream(int parameterIndex) throws java.sql.SQLException;

    java.io.Reader getCharacterStream(java.lang.String parameterName) throws java.sql.SQLException;

    java.io.Reader getNCharacterStream(int parameterIndex) throws java.sql.SQLException;

    java.io.Reader getNCharacterStream(java.lang.String parameterName) throws java.sql.SQLException;

    java.lang.Object getObject(int parameterIndex) throws java.sql.SQLException;

    java.lang.Object getObject(int parameterIndex, java.lang.Class map) throws java.sql.SQLException;

    java.lang.Object getObject(int parameterIndex, java.util.Map map) throws java.sql.SQLException;

    java.lang.Object getObject(java.lang.String parameterIndex) throws java.sql.SQLException;

    java.lang.Object getObject(java.lang.String parameterIndex, java.lang.Class map) throws java.sql.SQLException;

    java.lang.Object getObject(java.lang.String parameterIndex, java.util.Map map) throws java.sql.SQLException;

    java.lang.String getNString(int parameterIndex) throws java.sql.SQLException;

    java.lang.String getNString(java.lang.String parameterName) throws java.sql.SQLException;

    java.lang.String getString(int parameterIndex) throws java.sql.SQLException;

    java.lang.String getString(java.lang.String parameterName) throws java.sql.SQLException;

    java.math.BigDecimal getBigDecimal(int parameterIndex) throws java.sql.SQLException;

    java.math.BigDecimal getBigDecimal(int parameterIndex, int x) throws java.sql.SQLException;

    java.math.BigDecimal getBigDecimal(java.lang.String parameterName) throws java.sql.SQLException;

    java.net.URL getURL(int parameterIndex) throws java.sql.SQLException;

    java.net.URL getURL(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.Array getArray(int parameterIndex) throws java.sql.SQLException;

    java.sql.Array getArray(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.Blob getBlob(int parameterIndex) throws java.sql.SQLException;

    java.sql.Blob getBlob(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.Clob getClob(int parameterIndex) throws java.sql.SQLException;

    java.sql.Clob getClob(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.Date getDate(int parameterIndex) throws java.sql.SQLException;

    java.sql.Date getDate(int parameterIndex, java.util.Calendar x) throws java.sql.SQLException;

    java.sql.Date getDate(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.Date getDate(java.lang.String parameterName, java.util.Calendar x) throws java.sql.SQLException;

    java.sql.NClob getNClob(int parameterIndex) throws java.sql.SQLException;

    java.sql.NClob getNClob(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.Ref getRef(int parameterIndex) throws java.sql.SQLException;

    java.sql.Ref getRef(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.RowId getRowId(int parameterIndex) throws java.sql.SQLException;

    java.sql.RowId getRowId(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.SQLXML getSQLXML(int parameterIndex) throws java.sql.SQLException;

    java.sql.SQLXML getSQLXML(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.Time getTime(int parameterIndex) throws java.sql.SQLException;

    java.sql.Time getTime(int parameterIndex, java.util.Calendar x) throws java.sql.SQLException;

    java.sql.Time getTime(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.Time getTime(java.lang.String parameterName, java.util.Calendar x) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(int parameterIndex) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(int parameterIndex, java.util.Calendar x) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(java.lang.String parameterName) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(java.lang.String parameterName, java.util.Calendar x) throws java.sql.SQLException;

    long getLong(int parameterIndex) throws java.sql.SQLException;

    long getLong(java.lang.String parameterName) throws java.sql.SQLException;

    short getShort(int parameterIndex) throws java.sql.SQLException;

    short getShort(java.lang.String parameterName) throws java.sql.SQLException;

    void registerOutParameter(int parameterIndex, int sqlType) throws java.sql.SQLException;

    void registerOutParameter(int parameterIndex, int sqlType, int scale) throws java.sql.SQLException;

    void registerOutParameter(int parameterIndex, int sqlType, java.lang.String scale) throws java.sql.SQLException;

    default void registerOutParameter(int parameterIndex, java.sql.SQLType sqlType) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }

    default void registerOutParameter(int parameterIndex, java.sql.SQLType sqlType, int scale) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }

    default void registerOutParameter(int parameterIndex, java.sql.SQLType sqlType, java.lang.String scale) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }

    void registerOutParameter(java.lang.String parameterIndex, int sqlType) throws java.sql.SQLException;

    void registerOutParameter(java.lang.String parameterIndex, int sqlType, int scale) throws java.sql.SQLException;

    void registerOutParameter(java.lang.String parameterIndex, int sqlType, java.lang.String scale) throws java.sql.SQLException;

    default void registerOutParameter(java.lang.String parameterIndex, java.sql.SQLType sqlType) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }

    default void registerOutParameter(java.lang.String parameterIndex, java.sql.SQLType sqlType, int scale) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }

    default void registerOutParameter(java.lang.String parameterIndex, java.sql.SQLType sqlType, java.lang.String scale) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("registerOutParameter not implemented");
    }

    void setAsciiStream(java.lang.String parameterName, java.io.InputStream x) throws java.sql.SQLException;

    void setAsciiStream(java.lang.String parameterName, java.io.InputStream x, int scale) throws java.sql.SQLException;

    void setAsciiStream(java.lang.String parameterName, java.io.InputStream x, long length) throws java.sql.SQLException;

    void setBigDecimal(java.lang.String parameterName, java.math.BigDecimal x) throws java.sql.SQLException;

    void setBinaryStream(java.lang.String parameterName, java.io.InputStream x) throws java.sql.SQLException;

    void setBinaryStream(java.lang.String parameterName, java.io.InputStream x, int scale) throws java.sql.SQLException;

    void setBinaryStream(java.lang.String parameterName, java.io.InputStream x, long length) throws java.sql.SQLException;

    void setBlob(java.lang.String parameterName, java.io.InputStream x) throws java.sql.SQLException;

    void setBlob(java.lang.String parameterName, java.io.InputStream x, long length) throws java.sql.SQLException;

    void setBlob(java.lang.String parameterName, java.sql.Blob x) throws java.sql.SQLException;

    void setBoolean(java.lang.String parameterName, boolean x) throws java.sql.SQLException;

    void setByte(java.lang.String parameterName, byte x) throws java.sql.SQLException;

    void setBytes(java.lang.String parameterName, byte[] x) throws java.sql.SQLException;

    void setCharacterStream(java.lang.String parameterName, java.io.Reader x) throws java.sql.SQLException;

    void setCharacterStream(java.lang.String parameterName, java.io.Reader x, int scale) throws java.sql.SQLException;

    void setCharacterStream(java.lang.String parameterName, java.io.Reader x, long length) throws java.sql.SQLException;

    void setClob(java.lang.String parameterName, java.io.Reader x) throws java.sql.SQLException;

    void setClob(java.lang.String parameterName, java.io.Reader x, long length) throws java.sql.SQLException;

    void setClob(java.lang.String parameterName, java.sql.Clob x) throws java.sql.SQLException;

    void setDate(java.lang.String parameterName, java.sql.Date x) throws java.sql.SQLException;

    void setDate(java.lang.String parameterName, java.sql.Date x, java.util.Calendar cal) throws java.sql.SQLException;

    void setDouble(java.lang.String parameterName, double x) throws java.sql.SQLException;

    void setFloat(java.lang.String parameterName, float x) throws java.sql.SQLException;

    void setInt(java.lang.String parameterName, int x) throws java.sql.SQLException;

    void setLong(java.lang.String parameterName, long x) throws java.sql.SQLException;

    void setNCharacterStream(java.lang.String parameterName, java.io.Reader x) throws java.sql.SQLException;

    void setNCharacterStream(java.lang.String parameterName, java.io.Reader x, long length) throws java.sql.SQLException;

    void setNClob(java.lang.String parameterName, java.io.Reader x) throws java.sql.SQLException;

    void setNClob(java.lang.String parameterName, java.io.Reader x, long length) throws java.sql.SQLException;

    void setNClob(java.lang.String parameterName, java.sql.NClob x) throws java.sql.SQLException;

    void setNString(java.lang.String parameterName, java.lang.String x) throws java.sql.SQLException;

    void setNull(java.lang.String parameterName, int x) throws java.sql.SQLException;

    void setNull(java.lang.String parameterName, int x, java.lang.String typeName) throws java.sql.SQLException;

    void setObject(java.lang.String parameterName, java.lang.Object x) throws java.sql.SQLException;

    void setObject(java.lang.String parameterName, java.lang.Object x, int scale) throws java.sql.SQLException;

    void setObject(java.lang.String parameterName, java.lang.Object x, int scale, int scale) throws java.sql.SQLException;

    default void setObject(java.lang.String parameterName, java.lang.Object x, java.sql.SQLType sqlType) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setObject not implemented");
    }

    default void setObject(java.lang.String parameterName, java.lang.Object x, java.sql.SQLType sqlType, int scale) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setObject not implemented");
    }

    void setRowId(java.lang.String parameterName, java.sql.RowId x) throws java.sql.SQLException;

    void setSQLXML(java.lang.String parameterName, java.sql.SQLXML x) throws java.sql.SQLException;

    void setShort(java.lang.String parameterName, short x) throws java.sql.SQLException;

    void setString(java.lang.String parameterName, java.lang.String x) throws java.sql.SQLException;

    void setTime(java.lang.String parameterName, java.sql.Time x) throws java.sql.SQLException;

    void setTime(java.lang.String parameterName, java.sql.Time x, java.util.Calendar cal) throws java.sql.SQLException;

    void setTimestamp(java.lang.String parameterName, java.sql.Timestamp x) throws java.sql.SQLException;

    void setTimestamp(java.lang.String parameterName, java.sql.Timestamp x, java.util.Calendar cal) throws java.sql.SQLException;

    void setURL(java.lang.String parameterName, java.net.URL x) throws java.sql.SQLException;
}
