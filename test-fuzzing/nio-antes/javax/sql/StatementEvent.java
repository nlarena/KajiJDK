package javax.sql;

/**
 * KajiLibrary's javax.sql.StatementEvent -- something happened to a pooled prepared statement.
 *
 * <p>The source is the {@link PooledConnection} and not the statement, even though the event is
 * about it: it is the connection that has the listeners, and the statement travels as data.
 */
public class StatementEvent extends java.util.EventObject {

    private final java.sql.PreparedStatement statement;
    private final java.sql.SQLException ex;

    public StatementEvent(PooledConnection con, java.sql.PreparedStatement statement) {
        super(con);
        this.statement = statement;
        this.ex = null;
    }

    public StatementEvent(PooledConnection con, java.sql.PreparedStatement statement,
            java.sql.SQLException ex) {
        super(con);
        this.statement = statement;
        this.ex = ex;
    }

    /** The statement the event is about. */
    public java.sql.PreparedStatement getStatement() {
        return this.statement;
    }

    /** The error, or `null`. */
    public java.sql.SQLException getSQLException() {
        return this.ex;
    }
}
