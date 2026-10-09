package java.sql;

/**
 * KajiLibrary's java.sql.SQLTimeoutException -- a time limit ran out: the one
 * {@link Statement#setQueryTimeout} set, or a login timeout. (This note named only the first.)

 * <p>It is transient because the limit is the **caller's**, not the database's: the same query with
 * more time, or with the database less loaded, works.
 */
public class SQLTimeoutException extends SQLTransientException {

    public SQLTimeoutException() {
        super();
    }

    public SQLTimeoutException(String reason) {
        super(reason);
    }

    public SQLTimeoutException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLTimeoutException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLTimeoutException(Throwable cause) {
        super(cause);
    }

    public SQLTimeoutException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLTimeoutException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLTimeoutException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
