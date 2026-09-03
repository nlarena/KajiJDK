package java.sql;

/**
 * KajiLibrary's java.sql.SQLIntegrityConstraintViolationException -- Se violo una restriccion: una clave duplicada, una foranea sin destino, un `not null` nulo.

 * <p>Es la excepcion que mas conviene distinguir de las demas: casi siempre no es un error del
 * programa sino **un dato que el usuario mando dos veces**, y merece un mensaje y no una traza.
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
