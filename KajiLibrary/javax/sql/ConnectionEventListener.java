package javax.sql;

/**
 * KajiLibrary's javax.sql.ConnectionEventListener -- a connection pool listens to it.
 *
 * <p>It is the mechanism that makes a pool work without whoever uses the connection finding out:
 * the application calls `close()` on what it thinks is a connection, and what really happens is
 * that this notice arrives and the connection **goes back to the pool**.
 */
public interface ConnectionEventListener extends java.util.EventListener {

    /** The application closed its logical connection: the physical one can be reused. */
    void connectionClosed(ConnectionEvent event);

    /** The physical connection broke: it has to be discarded, not reused. */
    void connectionErrorOccurred(ConnectionEvent event);
}
