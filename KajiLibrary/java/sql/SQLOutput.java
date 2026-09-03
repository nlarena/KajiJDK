package java.sql;

/**
 * KajiLibrary's java.sql.SQLOutput -- el flujo en el que un {@link SQLData} escribe sus atributos.
 *
 * <p>El espejo exacto de {@link SQLInput}, y tiene que serlo: el orden de los `writeXxx` es el que
 * los `readXxx` van a suponer. Un atributo de mas o de menos no da error -- da un valor corrido.
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

    /** Escribe otro valor estructurado, que se serializa a su vez con su propio `writeSQL`. */
    void writeObject(SQLData x) throws SQLException;

    /** Escribe un objeto cualquiera diciendo con que tipo SQL. */
    default void writeObject(Object x, SQLType targetSqlType) throws SQLException {
        throw new SQLFeatureNotSupportedException("writeObject(Object, SQLType) no esta implementado");
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
