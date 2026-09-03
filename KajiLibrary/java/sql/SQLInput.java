package java.sql;

/**
 * KajiLibrary's java.sql.SQLInput -- el flujo del que un {@link SQLData} lee sus atributos.
 *
 * <p>Es un cursor sobre los atributos de **un** valor estructurado: cada `readXxx` consume el
 * siguiente y avanza. No hay como retroceder ni saltear, y no hace falta -- quien lee es la clase que
 * conoce el tipo.
 *
 * <p>{@link #wasNull} esta por lo mismo que en {@link ResultSet}: un `readInt` que devuelve cero no
 * distingue el cero del nulo.
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

    /** El siguiente atributo como objeto, resolviendo tipos propios por el mapa de la conexion. */
    Object readObject() throws SQLException;

    /** El siguiente atributo convertido a `type`; la forma con tipo, que evita el molde. */
    default <T> T readObject(Class<T> type) throws SQLException {
        throw new SQLFeatureNotSupportedException("readObject(Class) no esta implementado");
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

    /** Si el ultimo valor leido era nulo. */
    boolean wasNull() throws SQLException;
}
