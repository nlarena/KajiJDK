package java.sql;

/**
 * KajiLibrary's java.sql.SQLTransientException -- a failure that **may** go away if retried.

 * <p>It is the useful half of the split: whoever catches this knows that retrying makes sense,
 * without having to interpret vendor codes. The other half is {@link SQLNonTransientException}.
 */
public class SQLTransientException extends SQLException {

    public SQLTransientException() {
        super();
    }

    public SQLTransientException(String reason) {
        super(reason);
    }

    public SQLTransientException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLTransientException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLTransientException(Throwable cause) {
        super(cause);
    }

    public SQLTransientException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLTransientException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLTransientException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
