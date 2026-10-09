package java.sql;

/**
 * KajiLibrary's java.sql.SQLWarning -- something the database wanted to say without the operation
 * failing.
 *
 * <p>That it is an **exception that is not thrown** sounds odd and is deliberate: it inherits the
 * message, the `SQLState` and the chaining of {@link SQLException}, which is exactly what is needed
 * to report a warning, and in exchange avoids a second type with the same shape. It is collected by
 * asking, not by catching.
 */
public class SQLWarning extends SQLException {

    public SQLWarning() {
        super();
    }

    public SQLWarning(String reason) {
        super(reason);
    }

    public SQLWarning(String reason, String sqlState) {
        super(reason, sqlState);
    }

    public SQLWarning(String reason, String sqlState, int vendorCode) {
        super(reason, sqlState, vendorCode);
    }

    public SQLWarning(Throwable cause) {
        super(cause);
    }

    public SQLWarning(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLWarning(String reason, String sqlState, Throwable cause) {
        super(reason, sqlState, cause);
    }

    public SQLWarning(String reason, String sqlState, int vendorCode, Throwable cause) {
        super(reason, sqlState, vendorCode, cause);
    }

    /** The next warning in the chain, or `null`. */
    public SQLWarning getNextWarning() {
        SQLException next = this.getNextException();
        if (next == null) {
            return null;
        }
        if (next instanceof SQLWarning) {
            return (SQLWarning) next;
        }
        throw new Error("SQLWarning chain holds value that is not a SQLWarning");
    }

    /** Adds `w` at the end of the warning chain. */
    public void setNextWarning(SQLWarning w) {
        this.setNextException(w);
    }
}
