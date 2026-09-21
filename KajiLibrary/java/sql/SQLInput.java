package java.sql;

/**
 * KajiLibrary's java.sql.SQLInput -- the stream an {@link SQLData} reads its attributes from.
 *
 * <p>It is a cursor over the attributes of **one** structured value: each `readXxx` consumes the
 * next one and advances. There is no way to go back or skip, and there is no need -- whoever reads
 * is the class that knows the type.
 *
 * <p>{@link #wasNull} is there for the same reason as in {@link ResultSet}: a `readInt` that
 * returns zero does not tell zero from null.
 */
public interface SQLInput {

    String readString() throws SQLException;

    boolean readBoolean() throws SQLException;

    byte readByte() throws SQLException;

    short readShort() throws SQLException;

    int readInt() throws SQLException;

    long readLong() throws SQLException;

    float readFloat() throws SQLException;

    double readDouble() throws SQLException;

    java.math.BigDecimal readBigDecimal() throws SQLException;

    byte[] readBytes() throws SQLException;

    Date readDate() throws SQLException;

    Time readTime() throws SQLException;

    Timestamp readTimestamp() throws SQLException;

    java.io.Reader readCharacterStream() throws SQLException;

    java.io.InputStream readAsciiStream() throws SQLException;

    java.io.InputStream readBinaryStream() throws SQLException;

    /** The next attribute as an object, resolving types of its own through the connection's map. */
    Object readObject() throws SQLException;

    /** The next attribute converted to `type`; the typed form, which avoids the cast. */
    default <T> T readObject(Class<T> type) throws SQLException {
        throw new SQLFeatureNotSupportedException("readObject(Class) not implemented");
    }

    Ref readRef() throws SQLException;

    Blob readBlob() throws SQLException;

    Clob readClob() throws SQLException;

    Array readArray() throws SQLException;

    java.net.URL readURL() throws SQLException;

    NClob readNClob() throws SQLException;

    String readNString() throws SQLException;

    SQLXML readSQLXML() throws SQLException;

    RowId readRowId() throws SQLException;

    /** Whether the last value read was null. */
    boolean wasNull() throws SQLException;
}
