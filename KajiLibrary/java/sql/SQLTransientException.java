package java.sql;

/**
 * KajiLibrary's java.sql.SQLTransientException -- Fallo que **puede** desaparecer si se reintenta.

 * <p>Es la mitad util de la division: quien atrapa esto sabe que reintentar tiene sentido, sin tener
 * que interpretar codigos de proveedor. La otra mitad es {@link SQLNonTransientException}.
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
