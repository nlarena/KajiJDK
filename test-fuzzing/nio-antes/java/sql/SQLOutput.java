package java.sql;

/**
 * KajiLibrary's java.sql.SQLOutput -- the stream an {@link SQLData} writes its attributes to.
 *
 * <p>The exact mirror of {@link SQLInput}, and it has to be: the order of the `writeXxx` is the one
 * the `readXxx` will assume. One attribute too many or too few gives no error -- it gives a shifted
 * value.
 */
public interface SQLOutput {

    void writeString(String x) throws SQLException;

    void writeBoolean(boolean x) throws SQLException;

    void writeByte(byte x) throws SQLException;

    void writeShort(short x) throws SQLException;

    void writeInt(int x) throws SQLException;

    void writeLong(long x) throws SQLException;

    void writeFloat(float x) throws SQLException;

    void writeDouble(double x) throws SQLException;

    void writeBigDecimal(java.math.BigDecimal x) throws SQLException;

    void writeBytes(byte[] x) throws SQLException;

    void writeDate(Date x) throws SQLException;

    void writeTime(Time x) throws SQLException;

    void writeTimestamp(Timestamp x) throws SQLException;

    void writeCharacterStream(java.io.Reader x) throws SQLException;

    void writeAsciiStream(java.io.InputStream x) throws SQLException;

    void writeBinaryStream(java.io.InputStream x) throws SQLException;

    /** Writes another structured value, which is serialized in turn with its own `writeSQL`. */
    void writeObject(SQLData x) throws SQLException;

    /** Writes any object, saying with which SQL type. */
    default void writeObject(Object x, SQLType targetSqlType) throws SQLException {
        throw new SQLFeatureNotSupportedException("writeObject(Object, SQLType) not implemented");
    }

    void writeRef(Ref x) throws SQLException;

    void writeBlob(Blob x) throws SQLException;

    void writeClob(Clob x) throws SQLException;

    void writeStruct(Struct x) throws SQLException;

    void writeArray(Array x) throws SQLException;

    void writeURL(java.net.URL x) throws SQLException;

    void writeNString(String x) throws SQLException;

    void writeNClob(NClob x) throws SQLException;

    void writeRowId(RowId x) throws SQLException;

    void writeSQLXML(SQLXML x) throws SQLException;
}
