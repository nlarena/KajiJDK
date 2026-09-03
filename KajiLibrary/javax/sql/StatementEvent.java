package javax.sql;

/**
 * KajiLibrary's javax.sql.StatementEvent -- le paso algo a una sentencia preparada agrupada.
 *
 * <p>La fuente es la {@link PooledConnection} y no la sentencia, aunque el evento sea sobre ella: es
 * la conexion la que tiene los oyentes, y la sentencia viaja como dato.
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

    /** La sentencia de la que habla el evento. */
    public java.sql.PreparedStatement getStatement() {
        return this.statement;
    }

    /** El error, o `null`. */
    public java.sql.SQLException getSQLException() {
        return this.ex;
    }
}
