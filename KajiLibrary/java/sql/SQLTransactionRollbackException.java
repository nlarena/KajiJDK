package java.sql;

/**
 * KajiLibrary's java.sql.SQLTransactionRollbackException -- La base deshizo la transaccion sola.

 * <p>El caso tipico es un abrazo mortal: la base elige una victima y la deshace para que la otra
 * avance. Reintentar es exactamente lo que corresponde -- la segunda vez no hay con quien trabarse.
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
