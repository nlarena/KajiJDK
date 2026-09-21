package java.sql;

/**
 * KajiLibrary's java.sql.SQLNonTransientException -- a failure that retrying does **not** fix.
 *
 * <p>The split between this one and `SQLTransientException` is all they contribute: they tell
 * whoever catches whether it makes sense to try again. A syntax error does not improve by retrying;
 * a momentary lock does. Without this distinction, every retry layer would have to look at vendor
 * codes to decide.
 */
public class SQLNonTransientException extends SQLException {

    public SQLNonTransientException() {
        super();
    }

    public SQLNonTransientException(String reason) {
        super(reason);
    }

    public SQLNonTransientException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLNonTransientException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLNonTransientException(Throwable cause) {
        super(cause);
    }

    public SQLNonTransientException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLNonTransientException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLNonTransientException(String reason, String SQLState, int vendorCode,
            Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
