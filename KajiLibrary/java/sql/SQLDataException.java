package java.sql;

/**
 * KajiLibrary's java.sql.SQLDataException -- a value is not valid: out of range, of the wrong type,
 * or not convertible.
 */
public class SQLDataException extends SQLNonTransientException {

    public SQLDataException() {
        super();
    }

    public SQLDataException(String reason) {
        super(reason);
    }

    public SQLDataException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLDataException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLDataException(Throwable cause) {
        super(cause);
    }

    public SQLDataException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLDataException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLDataException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
