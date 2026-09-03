package java.sql;

/**
 * KajiLibrary's java.sql.PreparedStatement -- una sentencia con huecos, y los valores aparte.
 *
 * <p>Dos razones para preferirla siempre, y la segunda importa mas de lo que parece. La primera es
 * el rendimiento: la base analiza la sentencia una vez y la reutiliza con distintos valores. La
 * segunda es que **no hay inyeccion SQL posible**: el valor viaja por un canal distinto del texto de
 * la sentencia, asi que un valor que contenga `'; drop table` es un valor que contiene esos
 * caracteres y nunca codigo. No es que se escapen bien -- es que no se mezclan.
 *
 * <p>Los parametros se numeran **desde uno**, como las columnas.
 *
 * <p><strong>Subconjunto declarado.</strong> Estan los `setXxx` de los tipos que esta biblioteca
 * tiene y la ejecucion; quedan afuera los que reciben tipos SQL propios (`setBlob`, `setArray`,
 * `setSQLXML`, los flujos con longitud) por la misma razon que en {@link ResultSet}.
 */
public interface PreparedStatement extends Statement {

    /** Ejecuta la consulta con los parametros que tiene puestos. */
    ResultSet executeQuery() throws SQLException;

    /** Ejecuta la modificacion y devuelve cuantas filas toco. */
    int executeUpdate() throws SQLException;

    long executeLargeUpdate() throws SQLException;

    boolean execute() throws SQLException;

    /** Agrega los parametros actuales al lote. */
    void addBatch() throws SQLException;

    /** Olvida los parametros puestos. */
    void clearParameters() throws SQLException;

    /** Que columnas devolveria, **sin ejecutarla**. */
    ResultSetMetaData getMetaData() throws SQLException;

    // ---- los parametros ------------------------------------------------------------------------------

    /**
     * Pone nulo.
     *
     * <p>Pide el tipo porque un nulo tambien lo tiene: la base necesita saber de que columna es el
     * nulo para elegir el plan, y no puede deducirlo de un valor que no esta.
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

    // ---- el resto de los parametros ------------------------------------------------------------------
    //
    // La familia entera, y conviene ver por que es tan grande. Hay tres ejes que se multiplican: el
    // **tipo** del valor, si se pasa el dato o un puntero a el (`setBlob(int, Blob)` contra
    // `setBlob(int, InputStream)`), y si se dice cuanto mide. Las variantes con longitud existen
    // porque un driver que sabe el tamano de antemano puede reservarlo de una vez en lugar de ir
    // creciendo; las que no la piden llegaron despues, cuando quedo claro que el llamador casi nunca
    // la sabe.
    //
    // Los `setN*` son la version en juego de caracteres nacional, la misma distincion que separa
    // `NClob` de `Clob`.
    //
    // Y `setUnicodeStream` esta obsoleto desde 1999: recibia el texto en un UTF-16 propio de JDBC que
    // nunca quedo bien definido. Se declara porque la firma es el contrato, no porque haya que usarlo.

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
        throw new java.sql.SQLFeatureNotSupportedException("setObject no esta implementado");
    }

    default void setObject(int parameterIndex, java.lang.Object x, java.sql.SQLType targetSqlType, int scaleOrLength) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setObject no esta implementado");
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
