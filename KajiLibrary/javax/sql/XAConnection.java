package javax.sql;

/**
 * KajiLibrary's javax.sql.XAConnection -- una conexion que puede participar de una transaccion
 * distribuida.
 *
 * <p>Agrega **un** metodo sobre {@link PooledConnection}, y con eso alcanza: el
 * {@link javax.transaction.xa.XAResource} es la parte que el coordinador maneja, y la conexion sigue
 * usandose igual que cualquier otra. Lo que cambia es quien decide el `commit` -- ya no la
 * aplicacion sobre la conexion, sino el coordinador sobre el recurso.
 */
public interface XAConnection extends PooledConnection {

    /** El recurso con el que el coordinador maneja esta conexion. */
    javax.transaction.xa.XAResource getXAResource() throws java.sql.SQLException;
}
