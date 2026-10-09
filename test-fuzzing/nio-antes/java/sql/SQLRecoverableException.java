package java.sql;

/**
 * KajiLibrary's java.sql.SQLRecoverableException -- a failure that can be recovered from, but
 * **not** by retrying the same thing.

 * <p>The difference from {@link SQLTransientException} is subtle and real: here the connection was
 * left unusable, so a new one has to be made and the whole transaction redone. Retrying the
 * operation on the same connection will never work.
 */
public class SQLRecoverableException extends SQLException {

    public SQLRecoverableException() {
        super();
    }

    public SQLRecoverableException(String reason) {
        super(reason);
    }

    public SQLRecoverableException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLRecoverableException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLRecoverableException(Throwable cause) {
        super(cause);
    }

    public SQLRecoverableException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLRecoverableException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLRecoverableException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
