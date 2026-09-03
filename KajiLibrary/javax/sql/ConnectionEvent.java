package javax.sql;

/**
 * KajiLibrary's javax.sql.ConnectionEvent -- le paso algo a una conexion agrupada.
 *
 * <p>La fuente del evento es la {@link PooledConnection}, y la excepcion --si la hubo-- viaja aparte:
 * el mismo evento sirve para "se cerro bien" y para "se rompio", y quien escucha distingue por el
 * metodo que le llamaron, no por el contenido.
 */
public class ConnectionEvent extends java.util.EventObject {

    private final java.sql.SQLException ex;

    /** Un evento sin error. */
    public ConnectionEvent(PooledConnection con) {
        super(con);
        this.ex = null;
    }

    /** Un evento con el error que lo causo. */
    public ConnectionEvent(PooledConnection con, java.sql.SQLException ex) {
        super(con);
        this.ex = ex;
    }

    /** El error, o `null` si no lo hubo. */
    public java.sql.SQLException getSQLException() {
        return this.ex;
    }
}
