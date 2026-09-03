package java.sql;

/**
 * KajiLibrary's java.sql.ResultSet -- las filas que devolvio una consulta, recorridas de a una.
 *
 * <p>Un cursor y no una lista, y esa es la decision de diseno que explica toda la interfaz: el
 * resultado de una consulta puede no entrar en memoria, asi que se recorre pidiendo la fila
 * siguiente. De ahi que `next()` sea a la vez "avanza" y "hay mas" -- dos preguntas en un metodo,
 * porque separarlas obligaria a traer la fila dos veces.
 *
 * <p><strong>Subconjunto declarado.</strong> Estan la navegacion, el estado del cursor, los
 * accesores por indice y por nombre de los tipos que esta biblioteca tiene, y los metadatos. Quedan
 * afuera las familias de conversion que arrastran tipos SQL propios --`getBlob`, `getClob`,
 * `getArray`, `getRef`, `getSQLXML`, `getRowId`-- y la mitad de actualizacion (`updateXxx`), que solo
 * un driver puede honrar.
 *
 * <p>Las columnas se numeran **desde uno**.
 */
public interface ResultSet extends Wrapper, AutoCloseable {

    // ---- direccion, tipo y concurrencia --------------------------------------------------------------

    /** Las filas se van a leer hacia adelante. */
    int FETCH_FORWARD = 1000;

    /** Se van a leer hacia atras. */
    int FETCH_REVERSE = 1001;

    /** No se sabe en que orden. */
    int FETCH_UNKNOWN = 1002;

    /** El cursor solo va hacia adelante. */
    int TYPE_FORWARD_ONLY = 1003;

    /** Se puede mover en cualquier direccion; no ve cambios de otros. */
    int TYPE_SCROLL_INSENSITIVE = 1004;

    /** Se puede mover en cualquier direccion; **si** ve cambios de otros. */
    int TYPE_SCROLL_SENSITIVE = 1005;

    /** No se puede actualizar por el cursor. */
    int CONCUR_READ_ONLY = 1007;

    /** Se puede actualizar por el cursor. */
    int CONCUR_UPDATABLE = 1008;

    /** El cursor sobrevive a un `commit`. */
    int HOLD_CURSORS_OVER_COMMIT = 1;

    /** El cursor se cierra en el `commit`. */
    int CLOSE_CURSORS_AT_COMMIT = 2;

    // ---- navegacion ------------------------------------------------------------------------------------

    /** Avanza a la fila siguiente; `false` cuando no quedan. */
    boolean next() throws SQLException;

    /** Retrocede una fila. */
    boolean previous() throws SQLException;

    boolean first() throws SQLException;

    boolean last() throws SQLException;

    /** Antes de la primera: `next()` deja en la primera. */
    void beforeFirst() throws SQLException;

    /** Despues de la ultima. */
    void afterLast() throws SQLException;

    /** A la fila `row`; negativo cuenta desde el final. */
    boolean absolute(int row) throws SQLException;

    /** `rows` filas mas alla de donde esta. */
    boolean relative(int rows) throws SQLException;

    /** En que fila esta, desde uno; cero si no esta en ninguna. */
    int getRow() throws SQLException;

    boolean isBeforeFirst() throws SQLException;

    boolean isAfterLast() throws SQLException;

    boolean isFirst() throws SQLException;

    boolean isLast() throws SQLException;

    // ---- ciclo de vida -----------------------------------------------------------------------------

    void close() throws SQLException;

    boolean isClosed() throws SQLException;

    /**
     * Si el ultimo valor leido era nulo.
     *
     * <p>Hace falta porque los accesores primitivos no pueden devolver `null`: `getInt` de una
     * columna nula devuelve cero, que es indistinguible de un cero de verdad. Se pregunta **despues**
     * de leer, no antes.
     */
    boolean wasNull() throws SQLException;

    // ---- accesores por indice ------------------------------------------------------------------------

    String getString(int columnIndex) throws SQLException;

    boolean getBoolean(int columnIndex) throws SQLException;

    byte getByte(int columnIndex) throws SQLException;

    short getShort(int columnIndex) throws SQLException;

    int getInt(int columnIndex) throws SQLException;

    long getLong(int columnIndex) throws SQLException;

    float getFloat(int columnIndex) throws SQLException;

    double getDouble(int columnIndex) throws SQLException;

    byte[] getBytes(int columnIndex) throws SQLException;

    Object getObject(int columnIndex) throws SQLException;

    java.math.BigDecimal getBigDecimal(int columnIndex) throws SQLException;

    /** El valor convertido a `type`; la forma con tipo, que evita el molde. */
    <T> T getObject(int columnIndex, Class<T> type) throws SQLException;

    // ---- accesores por nombre ------------------------------------------------------------------------
    //
    // Los mismos por etiqueta de columna. Cuestan una busqueda mas que el indice y a cambio no se
    // rompen cuando alguien agrega una columna a la consulta.

    String getString(String columnLabel) throws SQLException;

    boolean getBoolean(String columnLabel) throws SQLException;

    byte getByte(String columnLabel) throws SQLException;

    short getShort(String columnLabel) throws SQLException;

    int getInt(String columnLabel) throws SQLException;

    long getLong(String columnLabel) throws SQLException;

    float getFloat(String columnLabel) throws SQLException;

    double getDouble(String columnLabel) throws SQLException;

    byte[] getBytes(String columnLabel) throws SQLException;

    Object getObject(String columnLabel) throws SQLException;

    java.math.BigDecimal getBigDecimal(String columnLabel) throws SQLException;

    <T> T getObject(String columnLabel, Class<T> type) throws SQLException;

    /** El indice de esa columna, desde uno. */
    int findColumn(String columnLabel) throws SQLException;

    // ---- forma y estado ------------------------------------------------------------------------------

    /** Que columnas hay. */
    ResultSetMetaData getMetaData() throws SQLException;

    /** La sentencia que produjo este resultado, o `null` si no la hubo. */
    Statement getStatement() throws SQLException;

    /** El nombre del cursor, para un `update ... where current of`. */
    String getCursorName() throws SQLException;

    int getType() throws SQLException;

    int getConcurrency() throws SQLException;

    int getHoldability() throws SQLException;

    void setFetchDirection(int direction) throws SQLException;

    int getFetchDirection() throws SQLException;

    /** Cuantas filas traer por viaje: una pista de rendimiento, no un limite. */
    void setFetchSize(int rows) throws SQLException;

    int getFetchSize() throws SQLException;

    SQLWarning getWarnings() throws SQLException;

    void clearWarnings() throws SQLException;

    // ---- el resto de los accesores, y la mitad de actualizacion --------------------------------------
    //
    // Los `getXxx` que faltaban son los tipos grandes y los de fecha. Las variantes con `Calendar`
    // existen por una razon concreta: una columna `TIMESTAMP` sin zona horaria no designa un instante
    // hasta que alguien elige la zona en que leerla, y sin este argumento esa eleccion la hace la
    // maquina que corre el programa -- lo cual convierte un dato en algo que cambia de servidor a
    // servidor. Las variantes con `Map` traducen tipos SQL propios a clases Java.
    //
    // Los `updateXxx` son la otra mitad de la interfaz, y la menos usada: un cursor actualizable deja
    // escribir **por la fila**, sin escribir un `update`. Se cambian los valores, se llama a
    // `updateRow`, y la base traduce. `insertRow` va con `moveToInsertRow`, que mueve el cursor a una
    // fila que todavia no existe.
    //
    // `getUnicodeStream` esta obsoleto desde 1999, igual que su gemelo en `PreparedStatement`.

    boolean rowDeleted() throws java.sql.SQLException;

    boolean rowInserted() throws java.sql.SQLException;

    boolean rowUpdated() throws java.sql.SQLException;

    java.io.InputStream getAsciiStream(int columnIndex) throws java.sql.SQLException;

    java.io.InputStream getAsciiStream(java.lang.String columnLabel) throws java.sql.SQLException;

    java.io.InputStream getBinaryStream(int columnIndex) throws java.sql.SQLException;

    java.io.InputStream getBinaryStream(java.lang.String columnLabel) throws java.sql.SQLException;

    java.io.InputStream getUnicodeStream(int columnIndex) throws java.sql.SQLException;

    java.io.InputStream getUnicodeStream(java.lang.String columnLabel) throws java.sql.SQLException;

    java.io.Reader getCharacterStream(int columnIndex) throws java.sql.SQLException;

    java.io.Reader getCharacterStream(java.lang.String columnLabel) throws java.sql.SQLException;

    java.io.Reader getNCharacterStream(int columnIndex) throws java.sql.SQLException;

    java.io.Reader getNCharacterStream(java.lang.String columnLabel) throws java.sql.SQLException;

    java.lang.Object getObject(int columnIndex, java.util.Map x) throws java.sql.SQLException;

    java.lang.Object getObject(java.lang.String columnLabel, java.util.Map x) throws java.sql.SQLException;

    java.lang.String getNString(int columnIndex) throws java.sql.SQLException;

    java.lang.String getNString(java.lang.String columnLabel) throws java.sql.SQLException;

    java.math.BigDecimal getBigDecimal(int columnIndex, int x) throws java.sql.SQLException;

    java.math.BigDecimal getBigDecimal(java.lang.String columnLabel, int x) throws java.sql.SQLException;

    java.net.URL getURL(int columnIndex) throws java.sql.SQLException;

    java.net.URL getURL(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Array getArray(int columnIndex) throws java.sql.SQLException;

    java.sql.Array getArray(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Blob getBlob(int columnIndex) throws java.sql.SQLException;

    java.sql.Blob getBlob(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Clob getClob(int columnIndex) throws java.sql.SQLException;

    java.sql.Clob getClob(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Date getDate(int columnIndex) throws java.sql.SQLException;

    java.sql.Date getDate(int columnIndex, java.util.Calendar cal) throws java.sql.SQLException;

    java.sql.Date getDate(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Date getDate(java.lang.String columnLabel, java.util.Calendar cal) throws java.sql.SQLException;

    java.sql.NClob getNClob(int columnIndex) throws java.sql.SQLException;

    java.sql.NClob getNClob(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Ref getRef(int columnIndex) throws java.sql.SQLException;

    java.sql.Ref getRef(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.RowId getRowId(int columnIndex) throws java.sql.SQLException;

    java.sql.RowId getRowId(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.SQLXML getSQLXML(int columnIndex) throws java.sql.SQLException;

    java.sql.SQLXML getSQLXML(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Time getTime(int columnIndex) throws java.sql.SQLException;

    java.sql.Time getTime(int columnIndex, java.util.Calendar cal) throws java.sql.SQLException;

    java.sql.Time getTime(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Time getTime(java.lang.String columnLabel, java.util.Calendar cal) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(int columnIndex) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(int columnIndex, java.util.Calendar cal) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(java.lang.String columnLabel) throws java.sql.SQLException;

    java.sql.Timestamp getTimestamp(java.lang.String columnLabel, java.util.Calendar cal) throws java.sql.SQLException;

    void cancelRowUpdates() throws java.sql.SQLException;

    void deleteRow() throws java.sql.SQLException;

    void insertRow() throws java.sql.SQLException;

    void moveToCurrentRow() throws java.sql.SQLException;

    void moveToInsertRow() throws java.sql.SQLException;

    void refreshRow() throws java.sql.SQLException;

    void updateArray(int columnIndex, java.sql.Array x) throws java.sql.SQLException;

    void updateArray(java.lang.String columnLabel, java.sql.Array x) throws java.sql.SQLException;

    void updateAsciiStream(int columnIndex, java.io.InputStream x) throws java.sql.SQLException;

    void updateAsciiStream(int columnIndex, java.io.InputStream x, int length) throws java.sql.SQLException;

    void updateAsciiStream(int columnIndex, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateAsciiStream(java.lang.String columnLabel, java.io.InputStream x) throws java.sql.SQLException;

    void updateAsciiStream(java.lang.String columnLabel, java.io.InputStream x, int length) throws java.sql.SQLException;

    void updateAsciiStream(java.lang.String columnLabel, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateBigDecimal(int columnIndex, java.math.BigDecimal x) throws java.sql.SQLException;

    void updateBigDecimal(java.lang.String columnLabel, java.math.BigDecimal x) throws java.sql.SQLException;

    void updateBinaryStream(int columnIndex, java.io.InputStream x) throws java.sql.SQLException;

    void updateBinaryStream(int columnIndex, java.io.InputStream x, int length) throws java.sql.SQLException;

    void updateBinaryStream(int columnIndex, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateBinaryStream(java.lang.String columnLabel, java.io.InputStream x) throws java.sql.SQLException;

    void updateBinaryStream(java.lang.String columnLabel, java.io.InputStream x, int length) throws java.sql.SQLException;

    void updateBinaryStream(java.lang.String columnLabel, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateBlob(int columnIndex, java.io.InputStream x) throws java.sql.SQLException;

    void updateBlob(int columnIndex, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateBlob(int columnIndex, java.sql.Blob x) throws java.sql.SQLException;

    void updateBlob(java.lang.String columnLabel, java.io.InputStream x) throws java.sql.SQLException;

    void updateBlob(java.lang.String columnLabel, java.io.InputStream x, long length) throws java.sql.SQLException;

    void updateBlob(java.lang.String columnLabel, java.sql.Blob x) throws java.sql.SQLException;

    void updateBoolean(int columnIndex, boolean x) throws java.sql.SQLException;

    void updateBoolean(java.lang.String columnLabel, boolean x) throws java.sql.SQLException;

    void updateByte(int columnIndex, byte x) throws java.sql.SQLException;

    void updateByte(java.lang.String columnLabel, byte x) throws java.sql.SQLException;

    void updateBytes(int columnIndex, byte[] x) throws java.sql.SQLException;

    void updateBytes(java.lang.String columnLabel, byte[] x) throws java.sql.SQLException;

    void updateCharacterStream(int columnIndex, java.io.Reader x) throws java.sql.SQLException;

    void updateCharacterStream(int columnIndex, java.io.Reader x, int length) throws java.sql.SQLException;

    void updateCharacterStream(int columnIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateCharacterStream(java.lang.String columnLabel, java.io.Reader x) throws java.sql.SQLException;

    void updateCharacterStream(java.lang.String columnLabel, java.io.Reader x, int length) throws java.sql.SQLException;

    void updateCharacterStream(java.lang.String columnLabel, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateClob(int columnIndex, java.io.Reader x) throws java.sql.SQLException;

    void updateClob(int columnIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateClob(int columnIndex, java.sql.Clob x) throws java.sql.SQLException;

    void updateClob(java.lang.String columnLabel, java.io.Reader x) throws java.sql.SQLException;

    void updateClob(java.lang.String columnLabel, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateClob(java.lang.String columnLabel, java.sql.Clob x) throws java.sql.SQLException;

    void updateDate(int columnIndex, java.sql.Date x) throws java.sql.SQLException;

    void updateDate(java.lang.String columnLabel, java.sql.Date x) throws java.sql.SQLException;

    void updateDouble(int columnIndex, double x) throws java.sql.SQLException;

    void updateDouble(java.lang.String columnLabel, double x) throws java.sql.SQLException;

    void updateFloat(int columnIndex, float x) throws java.sql.SQLException;

    void updateFloat(java.lang.String columnLabel, float x) throws java.sql.SQLException;

    void updateInt(int columnIndex, int x) throws java.sql.SQLException;

    void updateInt(java.lang.String columnLabel, int x) throws java.sql.SQLException;

    void updateLong(int columnIndex, long x) throws java.sql.SQLException;

    void updateLong(java.lang.String columnLabel, long x) throws java.sql.SQLException;

    void updateNCharacterStream(int columnIndex, java.io.Reader x) throws java.sql.SQLException;

    void updateNCharacterStream(int columnIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateNCharacterStream(java.lang.String columnLabel, java.io.Reader x) throws java.sql.SQLException;

    void updateNCharacterStream(java.lang.String columnLabel, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateNClob(int columnIndex, java.io.Reader x) throws java.sql.SQLException;

    void updateNClob(int columnIndex, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateNClob(int columnIndex, java.sql.NClob x) throws java.sql.SQLException;

    void updateNClob(java.lang.String columnLabel, java.io.Reader x) throws java.sql.SQLException;

    void updateNClob(java.lang.String columnLabel, java.io.Reader x, long length) throws java.sql.SQLException;

    void updateNClob(java.lang.String columnLabel, java.sql.NClob x) throws java.sql.SQLException;

    void updateNString(int columnIndex, java.lang.String x) throws java.sql.SQLException;

    void updateNString(java.lang.String columnLabel, java.lang.String x) throws java.sql.SQLException;

    void updateNull(int columnIndex) throws java.sql.SQLException;

    void updateNull(java.lang.String columnLabel) throws java.sql.SQLException;

    void updateObject(int columnIndex, java.lang.Object x) throws java.sql.SQLException;

    void updateObject(int columnIndex, java.lang.Object x, int scaleOrLength) throws java.sql.SQLException;

    default void updateObject(int columnIndex, java.lang.Object x, java.sql.SQLType targetSqlType) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("updateObject no esta implementado");
    }

    default void updateObject(int columnIndex, java.lang.Object x, java.sql.SQLType targetSqlType, int scaleOrLength) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("updateObject no esta implementado");
    }

    void updateObject(java.lang.String columnLabel, java.lang.Object x) throws java.sql.SQLException;

    void updateObject(java.lang.String columnLabel, java.lang.Object x, int scaleOrLength) throws java.sql.SQLException;

    default void updateObject(java.lang.String columnLabel, java.lang.Object x, java.sql.SQLType targetSqlType) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("updateObject no esta implementado");
    }

    default void updateObject(java.lang.String columnLabel, java.lang.Object x, java.sql.SQLType targetSqlType, int scaleOrLength) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("updateObject no esta implementado");
    }

    void updateRef(int columnIndex, java.sql.Ref x) throws java.sql.SQLException;

    void updateRef(java.lang.String columnLabel, java.sql.Ref x) throws java.sql.SQLException;

    void updateRow() throws java.sql.SQLException;

    void updateRowId(int columnIndex, java.sql.RowId x) throws java.sql.SQLException;

    void updateRowId(java.lang.String columnLabel, java.sql.RowId x) throws java.sql.SQLException;

    void updateSQLXML(int columnIndex, java.sql.SQLXML x) throws java.sql.SQLException;

    void updateSQLXML(java.lang.String columnLabel, java.sql.SQLXML x) throws java.sql.SQLException;

    void updateShort(int columnIndex, short x) throws java.sql.SQLException;

    void updateShort(java.lang.String columnLabel, short x) throws java.sql.SQLException;

    void updateString(int columnIndex, java.lang.String x) throws java.sql.SQLException;

    void updateString(java.lang.String columnLabel, java.lang.String x) throws java.sql.SQLException;

    void updateTime(int columnIndex, java.sql.Time x) throws java.sql.SQLException;

    void updateTime(java.lang.String columnLabel, java.sql.Time x) throws java.sql.SQLException;

    void updateTimestamp(int columnIndex, java.sql.Timestamp x) throws java.sql.SQLException;

    void updateTimestamp(java.lang.String columnLabel, java.sql.Timestamp x) throws java.sql.SQLException;
}
