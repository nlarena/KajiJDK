package java.sql;

/**
 * KajiLibrary's java.sql.Connection -- una sesion con una base de datos.
 *
 * <p><strong>Un subconjunto, y conviene decir cual.</strong> Estan los miembros que gobiernan la
 * **sesion**: el ciclo de vida (`close`/`isClosed`/`isValid`/`abort`), la transaccion
 * (`setAutoCommit`/`commit`/`rollback`/el nivel de aislamiento), el contexto (`catalog`/`schema`) y
 * los avisos. No estan los que **fabrican** otros objetos JDBC --`createStatement`,
 * `prepareStatement`, `getMetaData`, `createBlob`-- porque cada uno arrastra una interfaz de
 * cientos de miembros (`Statement`, `ResultSet`, `DatabaseMetaData`) que solo tiene sentido con un
 * driver detras.
 *
 * <p>Traer la interfaz sin driver es honesto justamente porque es una **interfaz**: un contrato no
 * promete que alguien lo cumpla. Lo que no se podria hacer es dar una implementacion que finja
 * conectarse.
 *
 * <p>Es `AutoCloseable`, que es la razon por la que una conexion se escribe casi siempre dentro de un
 * `try`-con-recursos: una conexion que se olvida de cerrar no se nota hasta que el pool se agota.
 */
public interface Connection extends Wrapper, AutoCloseable {

    // ---- niveles de aislamiento --------------------------------------------------------------------
    //
    // Los cuatro estan ordenados de menos a mas estricto, y cada escalon **quita** un fenomeno: el
    // primero deja leer lo que otra transaccion todavia no confirmo; el segundo lo impide pero deja
    // que un mismo `select` de dos valores distintos; el tercero lo impide pero deja aparecer filas
    // nuevas; el cuarto no deja nada. Mas estricto es mas correcto y mas lento, siempre.

    /** Sin transacciones. */
    int TRANSACTION_NONE = 0;

    /** Deja leer cambios que otra transaccion no confirmo. */
    int TRANSACTION_READ_UNCOMMITTED = 1;

    /** Solo lee lo confirmado; un mismo `select` puede dar distinto. */
    int TRANSACTION_READ_COMMITTED = 2;

    /** Un mismo `select` da lo mismo; pueden aparecer filas nuevas. */
    int TRANSACTION_REPEATABLE_READ = 4;

    /** Como si las transacciones corrieran de a una. */
    int TRANSACTION_SERIALIZABLE = 8;

    // ---- transaccion --------------------------------------------------------------------------------

    /**
     * Si cada sentencia se confirma sola.
     *
     * <p>Apagarlo es lo que **empieza** una transaccion: no hay un `begin`, hay un `setAutoCommit(false)`.
     */
    void setAutoCommit(boolean autoCommit) throws SQLException;

    boolean getAutoCommit() throws SQLException;

    /** Confirma lo hecho desde el ultimo `commit`/`rollback`. */
    void commit() throws SQLException;

    /** Descarta lo hecho desde el ultimo `commit`/`rollback`. */
    void rollback() throws SQLException;

    void setTransactionIsolation(int level) throws SQLException;

    int getTransactionIsolation() throws SQLException;

    /**
     * Anuncia que esta conexion **no** va a escribir.
     *
     * <p>Es una pista para que la base optimice, no una garantia que ella imponga -- y por eso solo
     * se puede fijar fuera de una transaccion.
     */
    void setReadOnly(boolean readOnly) throws SQLException;

    boolean isReadOnly() throws SQLException;

    // ---- ciclo de vida ------------------------------------------------------------------------------

    /** Cierra la conexion y suelta sus recursos. */
    void close() throws SQLException;

    /** Si ya se cerro. */
    boolean isClosed() throws SQLException;

    /**
     * Si la conexion **sigue viva**, esperando como mucho `timeout` segundos.
     *
     * <p>Distinto de `!isClosed()`: aquella pregunta si alguien la cerro de este lado, esta pregunta
     * si del otro lado sigue habiendo alguien. Un pool necesita la segunda.
     *
     * @param timeout segundos a esperar; cero para no poner limite
     */
    boolean isValid(int timeout) throws SQLException;

    /**
     * Cierra la conexion **desde afuera**, aunque este ocupada.
     *
     * <p>Es la salida para una conexion colgada: `close()` espera a que la operacion en curso
     * termine, y si esa operacion es la que quedo trabada, espera para siempre.
     */
    void abort(java.util.concurrent.Executor executor) throws SQLException;

    /** Cuanto puede tardar una operacion antes de que la conexion se cierre sola. */
    void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds)
            throws SQLException;

    int getNetworkTimeout() throws SQLException;

    // ---- contexto -----------------------------------------------------------------------------------

    void setCatalog(String catalog) throws SQLException;

    String getCatalog() throws SQLException;

    void setSchema(String schema) throws SQLException;

    String getSchema() throws SQLException;

    /** La sentencia traducida al dialecto de esta base. */
    String nativeSQL(String sql) throws SQLException;

    // ---- avisos --------------------------------------------------------------------------------------

    /** El primer aviso pendiente, o `null`. */
    SQLWarning getWarnings() throws SQLException;

    /** Descarta los avisos pendientes. */
    void clearWarnings() throws SQLException;

    // ---- fabricar sentencias -------------------------------------------------------------------------
    //
    // Las tres familias --`createStatement`, `prepareStatement`, `prepareCall`-- se corresponden con
    // las tres formas de mandar SQL, y cada una viene en cuatro tamanos: sin opciones, con tipo y
    // concurrencia del conjunto, con eso mas la retencion, y --solo la preparada-- con las claves
    // generadas. Es combinatoria, no diseno: cada version del estandar agrego un parametro y no podia
    // cambiar las firmas anteriores.
    //
    // Los `create*` de tipos grandes fabrican un {@link Blob}/{@link Clob} **vacio** del lado de la
    // base, para llenarlo antes de escribirlo; sin ellos habria que armar el valor entero en memoria,
    // que es justo lo que esos tipos evitan.
    //
    // Los puntos de guardado, la informacion de cliente y las claves de particion completan lo que
    // faltaba de la sesion.

    default boolean setShardingKeyIfValid(java.sql.ShardingKey shardingKey, int timeout) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setShardingKeyIfValid no esta implementado");
    }

    default boolean setShardingKeyIfValid(java.sql.ShardingKey shardingKey, java.sql.ShardingKey superShardingKey, int timeout) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setShardingKeyIfValid no esta implementado");
    }

    int getHoldability() throws java.sql.SQLException;

    java.lang.String getClientInfo(java.lang.String name) throws java.sql.SQLException;

    java.sql.Array createArrayOf(java.lang.String typeName, java.lang.Object[] elements) throws java.sql.SQLException;

    java.sql.Blob createBlob() throws java.sql.SQLException;

    java.sql.CallableStatement prepareCall(java.lang.String sql) throws java.sql.SQLException;

    java.sql.CallableStatement prepareCall(java.lang.String sql, int resultSetType, int resultSetConcurrency) throws java.sql.SQLException;

    java.sql.CallableStatement prepareCall(java.lang.String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws java.sql.SQLException;

    java.sql.Clob createClob() throws java.sql.SQLException;

    java.sql.DatabaseMetaData getMetaData() throws java.sql.SQLException;

    java.sql.NClob createNClob() throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql) throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql, int autoGeneratedKeys) throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql, int resultSetType, int resultSetConcurrency) throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql, int[] columnIndexes) throws java.sql.SQLException;

    java.sql.PreparedStatement prepareStatement(java.lang.String sql, java.lang.String[] columnNames) throws java.sql.SQLException;

    java.sql.SQLXML createSQLXML() throws java.sql.SQLException;

    java.sql.Savepoint setSavepoint() throws java.sql.SQLException;

    java.sql.Savepoint setSavepoint(java.lang.String name) throws java.sql.SQLException;

    java.sql.Statement createStatement() throws java.sql.SQLException;

    java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws java.sql.SQLException;

    java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws java.sql.SQLException;

    java.sql.Struct createStruct(java.lang.String typeName, java.lang.Object[] attributes) throws java.sql.SQLException;

    java.util.Map getTypeMap() throws java.sql.SQLException;

    java.util.Properties getClientInfo() throws java.sql.SQLException;

    default void beginRequest() throws java.sql.SQLException {
        // Sin agrupacion de pedidos: no hay nada que empezar.
    }

    default void endRequest() throws java.sql.SQLException {
        // Sin agrupacion de pedidos: no hay nada que terminar.
    }

    void releaseSavepoint(java.sql.Savepoint savepoint) throws java.sql.SQLException;

    void rollback(java.sql.Savepoint savepoint) throws java.sql.SQLException;

    void setClientInfo(java.lang.String name, java.lang.String value) throws java.sql.SQLClientInfoException;

    void setClientInfo(java.util.Properties name) throws java.sql.SQLClientInfoException;

    void setHoldability(int holdability) throws java.sql.SQLException;

    default void setShardingKey(java.sql.ShardingKey shardingKey) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setShardingKey no esta implementado");
    }

    default void setShardingKey(java.sql.ShardingKey shardingKey, java.sql.ShardingKey superShardingKey) throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("setShardingKey no esta implementado");
    }

    void setTypeMap(java.util.Map map) throws java.sql.SQLException;
}
