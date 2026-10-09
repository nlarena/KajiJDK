package java.sql;

/**
 * KajiLibrary's java.sql.SQLNonTransientConnectionException -- the connection failed for something
 * that will not change: the URL, the credentials, the server.
 */
public class SQLNonTransientConnectionException extends SQLNonTransientException {

    public SQLNonTransientConnectionException() {
        super();
    }

    public SQLNonTransientConnectionException(String reason) {
        super(reason);
    }

    public SQLNonTransientConnectionException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLNonTransientConnectionException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLNonTransientConnectionException(Throwable cause) {
        super(cause);
    }

    public SQLNonTransientConnectionException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLNonTransientConnectionException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLNonTransientConnectionException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
