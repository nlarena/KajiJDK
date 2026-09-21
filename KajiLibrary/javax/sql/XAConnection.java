package javax.sql;

/**
 * KajiLibrary's javax.sql.XAConnection -- a connection that can take part in a distributed
 * transaction.
 *
 * <p>It adds **one** method over {@link PooledConnection}, and that is enough: the {@link
 * javax.transaction.xa.XAResource} is the part the coordinator handles, and the connection is still
 * used like any other. What changes is who decides the `commit` -- no longer the application on the
 * connection, but the coordinator on the resource.
 */
public interface XAConnection extends PooledConnection {

    /** The resource the coordinator handles this connection with. */
    javax.transaction.xa.XAResource getXAResource() throws java.sql.SQLException;
}
