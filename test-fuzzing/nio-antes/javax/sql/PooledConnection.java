package javax.sql;

/**
 * KajiLibrary's javax.sql.PooledConnection -- the **physical** connection the pool keeps.
 *
 * <p>The distinction between this one and {@link java.sql.Connection} is the whole point: this is
 * the real connection, the one that is expensive to open; the one {@link #getConnection} returns is
 * a wrapper the application uses and closes without the physical one closing. That is why `close()`
 * is here --the pool calls it, not the application-- and why it does not extend `Connection`: it is
 * not one, it is the factory of the ones the application sees.
 */
public interface PooledConnection {

    /** A logical connection over this physical one. */
    java.sql.Connection getConnection() throws java.sql.SQLException;

    /** Closes the **physical** connection. The pool calls it. */
    void close() throws java.sql.SQLException;

    void addConnectionEventListener(ConnectionEventListener listener);

    void removeConnectionEventListener(ConnectionEventListener listener);

    void addStatementEventListener(StatementEventListener listener);

    void removeStatementEventListener(StatementEventListener listener);
}
