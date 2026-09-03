package java.sql;

/**
 * KajiLibrary's java.sql.SQLTimeoutException -- Se agoto el tiempo que {@link Statement#setQueryTimeout} habia fijado.

 * <p>Es transitoria porque el limite es del **llamador**, no de la base: la misma consulta con mas
 * tiempo, o con la base menos cargada, anda.
 */
public class SQLTimeoutException extends SQLTransientException {

    public SQLTimeoutException() {
        super();
    }

    public SQLTimeoutException(String reason) {
        super(reason);
    }

    public SQLTimeoutException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLTimeoutException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLTimeoutException(Throwable cause) {
        super(cause);
    }

    public SQLTimeoutException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLTimeoutException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLTimeoutException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
