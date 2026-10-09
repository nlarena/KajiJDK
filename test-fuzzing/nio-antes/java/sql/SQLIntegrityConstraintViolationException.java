package java.sql;

/**
 * KajiLibrary's java.sql.SQLIntegrityConstraintViolationException -- a constraint was violated: a
 * duplicate key, a foreign key with no target, a null in a `not null` column.

 * <p>It is the exception most worth telling apart from the rest: often it is not a program error
 * but **a value the user sent twice**, and it deserves a message and not a stack trace.
 */
public class SQLIntegrityConstraintViolationException extends SQLNonTransientException {

    public SQLIntegrityConstraintViolationException() {
        super();
    }

    public SQLIntegrityConstraintViolationException(String reason) {
        super(reason);
    }

    public SQLIntegrityConstraintViolationException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLIntegrityConstraintViolationException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLIntegrityConstraintViolationException(Throwable cause) {
        super(cause);
    }

    public SQLIntegrityConstraintViolationException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLIntegrityConstraintViolationException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLIntegrityConstraintViolationException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
