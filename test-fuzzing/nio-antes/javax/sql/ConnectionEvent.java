package javax.sql;

/**
 * KajiLibrary's javax.sql.ConnectionEvent -- something happened to a pooled connection.
 *
 * <p>The source of the event is the {@link PooledConnection}, and the exception --if there was
 * one-- travels separately: the same event serves for "it closed fine" and for "it broke", and the
 * listener tells them apart by the method it was called on, not by the content.
 */
public class ConnectionEvent extends java.util.EventObject {

    private final java.sql.SQLException ex;

    /** An event without an error. */
    public ConnectionEvent(PooledConnection con) {
        super(con);
        this.ex = null;
    }

    /** An event with the error that caused it. */
    public ConnectionEvent(PooledConnection con, java.sql.SQLException ex) {
        super(con);
        this.ex = ex;
    }

    /** The error, or `null` if there was none. */
    public java.sql.SQLException getSQLException() {
        return this.ex;
    }
}
