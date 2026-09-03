package javax.sql;

/**
 * KajiLibrary's javax.sql.ConnectionEventListener -- lo escucha un pool de conexiones.
 *
 * <p>Es el mecanismo que hace que un pool funcione sin que quien usa la conexion se entere: la
 * aplicacion llama a `close()` sobre lo que cree que es una conexion, y lo que pasa de verdad es que
 * llega este aviso y la conexion **vuelve al pool**.
 */
public interface ConnectionEventListener extends java.util.EventListener {

    /** La aplicacion cerro su conexion logica: la fisica se puede reutilizar. */
    void connectionClosed(ConnectionEvent event);

    /** La conexion fisica se rompio: hay que descartarla, no reutilizarla. */
    void connectionErrorOccurred(ConnectionEvent event);
}
