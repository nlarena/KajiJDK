package java.sql;

/**
 * KajiLibrary's java.sql.SQLRecoverableException -- Fallo del que se puede volver, pero **no** reintentando lo mismo.

 * <p>La diferencia con {@link SQLTransientException} es sutil y real: aca la conexion quedo
 * inservible, asi que hay que armar una nueva y rehacer la transaccion entera. Reintentar la
 * operacion sobre la misma conexion no va a andar nunca.
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
