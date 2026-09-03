package javax.sql;

/**
 * KajiLibrary's javax.sql.PooledConnection -- la conexion **fisica** que el pool guarda.
 *
 * <p>La distincion entre esta y {@link java.sql.Connection} es todo el asunto: esta es la conexion de
 * verdad, la que cuesta abrir; la que {@link #getConnection} devuelve es un envoltorio que la
 * aplicacion usa y cierra sin que la fisica se cierre. Por eso `close()` esta aca --lo llama el
 * pool, no la aplicacion-- y por eso no extiende `Connection`: no es una, es la fabrica de las que
 * la aplicacion ve.
 */
public interface PooledConnection {

    /** Una conexion logica sobre esta fisica. */
    java.sql.Connection getConnection() throws java.sql.SQLException;

    /** Cierra la conexion **fisica**. Lo llama el pool. */
    void close() throws java.sql.SQLException;

    void addConnectionEventListener(ConnectionEventListener listener);

    void removeConnectionEventListener(ConnectionEventListener listener);

    void addStatementEventListener(StatementEventListener listener);

    void removeStatementEventListener(StatementEventListener listener);
}
