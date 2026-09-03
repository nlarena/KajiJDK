package javax.sql;

import java.io.PrintWriter;

/**
 * KajiLibrary's javax.sql.CommonDataSource -- lo que toda fabrica de conexiones sabe hacer.
 *
 * <p>Existe para no repetir tres veces los mismos cuatro metodos: `DataSource`,
 * `ConnectionPoolDataSource` y `XADataSource` fabrican cosas distintas y comparten la configuracion
 * --a donde loguear y cuanto esperar--.
 */
public interface CommonDataSource {

    /** A donde van los mensajes de esta fuente, o `null` si no se fijo ninguno. */
    PrintWriter getLogWriter() throws java.sql.SQLException;

    /** Fija a donde van. */
    void setLogWriter(PrintWriter out) throws java.sql.SQLException;

    /**
     * Cuantos segundos esperar al abrir una conexion; cero para el limite del sistema.
     */
    void setLoginTimeout(int seconds) throws java.sql.SQLException;

    int getLoginTimeout() throws java.sql.SQLException;

    /**
     * El logger del que cuelgan los de esta fuente.
     *
     * <p>Devuelve el **padre** y no el propio a proposito: quien configura la traza quiere apagar o
     * subir de nivel a toda la familia de una fuente de un solo movimiento, y no puede conocer los
     * nombres que el driver eligio para los suyos.
     *
     * @throws java.sql.SQLFeatureNotSupportedException si la fuente no usa `java.util.logging`
     */
    java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException;

    /** Un constructor de claves de particion para esta fuente. */
    default java.sql.ShardingKeyBuilder createShardingKeyBuilder() throws java.sql.SQLException {
        throw new UnsupportedOperationException("createShardingKeyBuilder no esta implementado");
    }
}
