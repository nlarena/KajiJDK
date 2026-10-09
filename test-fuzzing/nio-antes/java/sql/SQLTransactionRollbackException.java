package java.sql;

/**
 * KajiLibrary's java.sql.SQLTransactionRollbackException -- the database rolled the transaction
 * back by itself.

 * <p>The typical case is a deadlock: the database picks a victim and rolls it back so the other can
 * proceed. Retrying is exactly the right thing -- the second time there is no one to deadlock with.
 */
public class SQLTransactionRollbackException extends SQLTransientException {

    public SQLTransactionRollbackException() {
        super();
    }

    public SQLTransactionRollbackException(String reason) {
        super(reason);
    }

    public SQLTransactionRollbackException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLTransactionRollbackException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLTransactionRollbackException(Throwable cause) {
        super(cause);
    }

    public SQLTransactionRollbackException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLTransactionRollbackException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLTransactionRollbackException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
