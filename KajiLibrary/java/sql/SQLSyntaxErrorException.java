package java.sql;

/**
 * KajiLibrary's java.sql.SQLSyntaxErrorException -- La sentencia esta mal escrita, o nombra algo que no existe.
 */
public class SQLSyntaxErrorException extends SQLNonTransientException {

    public SQLSyntaxErrorException() {
        super();
    }

    public SQLSyntaxErrorException(String reason) {
        super(reason);
    }

    public SQLSyntaxErrorException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLSyntaxErrorException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLSyntaxErrorException(Throwable cause) {
        super(cause);
    }

    public SQLSyntaxErrorException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLSyntaxErrorException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLSyntaxErrorException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
