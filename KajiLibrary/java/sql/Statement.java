package java.sql;

/**
 * KajiLibrary's java.sql.Statement -- una sentencia SQL que se manda como texto.
 *
 * <p><strong>Subconjunto declarado.</strong> Estan la ejecucion, los lotes, los limites y el ciclo de
 * vida. Quedan afuera las variantes que devuelven claves generadas y los `getMoreResults` con
 * banderas, que dependen de tipos y comportamientos que solo un driver define.
 *
 * <p>Que reciba la sentencia como texto es lo que la hace peligrosa: concatenar un valor del usuario
 * dentro de ese texto es exactamente la inyeccion SQL. Para valores esta {@link PreparedStatement},
 * que los manda **aparte** de la sentencia y por eso no puede confundirlos con codigo.
 */
public interface Statement extends Wrapper, AutoCloseable {

    /** No devolver las claves que la base genero. */
    int NO_GENERATED_KEYS = 2;

    /** Devolverlas. */
    int RETURN_GENERATED_KEYS = 1;

    /** Cerrar los resultados abiertos antes de traer el siguiente. */
    int CLOSE_CURRENT_RESULT = 1;

    /** Dejarlos abiertos. */
    int KEEP_CURRENT_RESULT = 2;

    /** Cerrar todos los resultados de esta sentencia. */
    int CLOSE_ALL_RESULTS = 3;

    /** La sentencia se ejecuto bien y no devolvio filas. */
    int SUCCESS_NO_INFO = -2;

    /** Esa sentencia del lote fallo. */
    int EXECUTE_FAILED = -3;

    /** Ejecuta una consulta y devuelve sus filas. */
    ResultSet executeQuery(String sql) throws SQLException;

    /** Ejecuta una modificacion y devuelve cuantas filas toco. */
    int executeUpdate(String sql) throws SQLException;

    /** Igual, para cuentas que no entran en un `int`. */
    long executeLargeUpdate(String sql) throws SQLException;

    /**
     * Ejecuta cualquier sentencia.
     *
     * @return `true` si lo primero que devolvio son filas; entonces se pide con {@link #getResultSet}
     */
    boolean execute(String sql) throws SQLException;

    /** Las filas del resultado actual, o `null` si el actual es una cuenta. */
    ResultSet getResultSet() throws SQLException;

    /** La cuenta del resultado actual, o -1 si el actual son filas. */
    int getUpdateCount() throws SQLException;

    /** Pasa al resultado siguiente. */
    boolean getMoreResults() throws SQLException;

    // ---- lotes ---------------------------------------------------------------------------------------
    //
    // Existen por la latencia: mandar mil `insert` de a uno son mil viajes de ida y vuelta. El lote
    // los manda juntos, y por eso devuelve **un arreglo** de cuentas y no una sola.

    void addBatch(String sql) throws SQLException;

    void clearBatch() throws SQLException;

    int[] executeBatch() throws SQLException;

    long[] executeLargeBatch() throws SQLException;

    // ---- limites -------------------------------------------------------------------------------------

    /** El maximo de bytes que devuelve una columna grande; cero para sin limite. */
    void setMaxFieldSize(int max) throws SQLException;

    int getMaxFieldSize() throws SQLException;

    /** El maximo de filas; cero para sin limite. */
    void setMaxRows(int max) throws SQLException;

    int getMaxRows() throws SQLException;

    /** Segundos antes de cancelar; cero para sin limite. */
    void setQueryTimeout(int seconds) throws SQLException;

    int getQueryTimeout() throws SQLException;

    void setFetchSize(int rows) throws SQLException;

    int getFetchSize() throws SQLException;

    void setFetchDirection(int direction) throws SQLException;

    int getFetchDirection() throws SQLException;

    /** Si la base debe interpretar las secuencias de escape `{fn ...}`. */
    void setEscapeProcessing(boolean enable) throws SQLException;

    /** El nombre del cursor de los resultados que produzca. */
    void setCursorName(String name) throws SQLException;

    // ---- ciclo de vida -------------------------------------------------------------------------------

    /** Cancela la ejecucion en curso, **desde otro hilo**. */
    void cancel() throws SQLException;

    void close() throws SQLException;

    boolean isClosed() throws SQLException;

    /** Que se cierre sola cuando se cierre su ultimo resultado. */
    void closeOnCompletion() throws SQLException;

    boolean isCloseOnCompletion() throws SQLException;

    /** Una pista de que no se va a reutilizar, para que el pool la descarte. */
    default void setPoolable(boolean poolable) throws SQLException {
        throw new UnsupportedOperationException("setPoolable no esta implementado");
    }

    default boolean isPoolable() throws SQLException {
        throw new UnsupportedOperationException("isPoolable no esta implementado");
    }

    int getResultSetType() throws SQLException;

    int getResultSetConcurrency() throws SQLException;

    int getResultSetHoldability() throws SQLException;

    /** La conexion que la creo. */
    Connection getConnection() throws SQLException;

    SQLWarning getWarnings() throws SQLException;

    void clearWarnings() throws SQLException;

    // ---- claves generadas y citado -------------------------------------------------------------------
    //
    // Las variantes de `execute`/`executeUpdate` con un segundo argumento son todas la misma pregunta:
    // que hacer con las claves que la base genero sola. Se pueden pedir todas
    // ({@link #RETURN_GENERATED_KEYS}), o nombrar cuales interesan por indice o por nombre de columna;
    // despues se leen con {@link #getGeneratedKeys}. Sin esto habria que hacer un `select` extra y
    // adivinar cual fila es la recien insertada.
    //
    // Los `enquote*` son la respuesta tardia --Java 9-- a que la unica forma de meter un identificador
    // en una sentencia era concatenarlo. Siguen sin ser tan seguros como un parametro: un
    // identificador **no** puede ser un parametro, asi que citar bien es lo mejor que se puede hacer.
    //
    // Los `large*` duplican metodos que devolvian `int` porque una tabla puede tener mas de dos mil
    // millones de filas, y el `int` los desbordaba en silencio.

    boolean execute(java.lang.String sql, int autoGeneratedKeys) throws java.sql.SQLException;

    boolean execute(java.lang.String sql, int[] columnIndexes) throws java.sql.SQLException;

    boolean execute(java.lang.String sql, java.lang.String[] columnNames) throws java.sql.SQLException;

    boolean getMoreResults(int current) throws java.sql.SQLException;

    default boolean isSimpleIdentifier(java.lang.String identifier) throws java.sql.SQLException {
        if (identifier == null || identifier.length() == 0 || identifier.length() > 128) {
            return false;
        }
        // Simple = empieza con letra y sigue con letras, digitos o guion bajo. Nada mas alcanza para
        // ir en una sentencia sin comillas.
        char c = identifier.charAt(0);
        if (!Character.isLetter(c)) {
            return false;
        }
        int i = 1;
        while (i < identifier.length()) {
            char d = identifier.charAt(i);
            if (!Character.isLetterOrDigit(d) && d != '_') {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    int executeUpdate(java.lang.String sql, int autoGeneratedKeys) throws java.sql.SQLException;

    int executeUpdate(java.lang.String sql, int[] columnIndexes) throws java.sql.SQLException;

    int executeUpdate(java.lang.String sql, java.lang.String[] columnNames) throws java.sql.SQLException;

    default java.lang.String enquoteIdentifier(java.lang.String identifier, boolean alwaysQuote) throws java.sql.SQLException {
        if (identifier == null || identifier.length() == 0 || identifier.length() > 128) {
            throw new java.sql.SQLException("identificador invalido");
        }
        if (!alwaysQuote && this.isSimpleIdentifier(identifier)) {
            return identifier;
        }
        // Un identificador va entre comillas **dobles**; una comilla doble adentro se duplica.
        // Un cero adentro no se puede citar de ninguna forma.
        if (identifier.indexOf('\u0000') >= 0) {
            throw new java.sql.SQLException("el identificador tiene un nulo");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    default java.lang.String enquoteLiteral(java.lang.String val) throws java.sql.SQLException {
        // Una comilla simple adentro se escribe duplicandola. Es la unica regla, y es la que hace
        // segura la operacion.
        return "'" + val.replace("'", "''") + "'";
    }

    default java.lang.String enquoteNCharLiteral(java.lang.String val) throws java.sql.SQLException {
        return "N" + this.enquoteLiteral(val);
    }

    java.sql.ResultSet getGeneratedKeys() throws java.sql.SQLException;

    default long executeLargeUpdate(java.lang.String sql, int autoGeneratedKeys) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("executeLargeUpdate no esta implementado");
    }

    default long executeLargeUpdate(java.lang.String sql, int[] columnIndexes) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("executeLargeUpdate no esta implementado");
    }

    default long executeLargeUpdate(java.lang.String sql, java.lang.String[] columnNames) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("executeLargeUpdate no esta implementado");
    }

    default long getLargeMaxRows() throws java.sql.SQLException {
        return (long) this.getMaxRows();
    }

    default long getLargeUpdateCount() throws java.sql.SQLException {
        throw new UnsupportedOperationException("getLargeUpdateCount no esta implementado");
    }

    default void setLargeMaxRows(long max) throws java.sql.SQLException {
        throw new UnsupportedOperationException("setLargeMaxRows no esta implementado");
    }
}
