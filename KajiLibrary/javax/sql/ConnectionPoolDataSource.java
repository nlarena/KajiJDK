package javax.sql;

/**
 * KajiLibrary's javax.sql.ConnectionPoolDataSource -- de donde salen las conexiones **fisicas**.
 *
 * <p>La implementa el driver; la usa el pool, no la aplicacion. La aplicacion ve un
 * {@link DataSource} que por dentro va a buscar aca. Esa division es la que permite que el pool lo
 * escriba alguien distinto del driver.
 */
public interface ConnectionPoolDataSource extends CommonDataSource {

    /** Una conexion fisica, con las credenciales configuradas. */
    PooledConnection getPooledConnection() throws java.sql.SQLException;

    /** Una conexion fisica con esas credenciales. */
    PooledConnection getPooledConnection(String user, String password)
            throws java.sql.SQLException;

    /** Un constructor, para pedir una con mas datos que usuario y clave. */
    default PooledConnectionBuilder createPooledConnectionBuilder() throws java.sql.SQLException {
        throw new UnsupportedOperationException("createPooledConnectionBuilder no esta implementado");
    }
}
