package java.sql;

/**
 * KajiLibrary's java.sql.SQLClientInfoException -- fallo al fijar propiedades del cliente.
 *
 * <p>Es la unica excepcion de JDBC que lleva un **mapa** en vez de un solo motivo, y por una razon
 * concreta: `setClientInfo` recibe varias propiedades juntas y puede fallar en algunas. Un motivo
 * suelto obligaria a lanzar en la primera que falla y a no decir nada de las demas.
 */
public class SQLClientInfoException extends SQLException {

    private final java.util.Map<String, ClientInfoStatus> failedProperties;

    public SQLClientInfoException() {
        super();
        this.failedProperties = null;
    }

    public SQLClientInfoException(java.util.Map<String, ClientInfoStatus> failedProperties) {
        super();
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(java.util.Map<String, ClientInfoStatus> failedProperties,
            Throwable cause) {
        super(cause != null ? cause.toString() : null, null, 0, cause);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason,
            java.util.Map<String, ClientInfoStatus> failedProperties) {
        super(reason);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason,
            java.util.Map<String, ClientInfoStatus> failedProperties, Throwable cause) {
        super(reason, null, 0, cause);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason, String SQLState,
            java.util.Map<String, ClientInfoStatus> failedProperties) {
        super(reason, SQLState);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason, String SQLState,
            java.util.Map<String, ClientInfoStatus> failedProperties, Throwable cause) {
        super(reason, SQLState, 0, cause);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason, String SQLState, int vendorCode,
            java.util.Map<String, ClientInfoStatus> failedProperties) {
        super(reason, SQLState, vendorCode);
        this.failedProperties = failedProperties;
    }

    public SQLClientInfoException(String reason, String SQLState, int vendorCode,
            java.util.Map<String, ClientInfoStatus> failedProperties, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
        this.failedProperties = failedProperties;
    }

    /** Que propiedad fallo y por que. */
    public java.util.Map<String, ClientInfoStatus> getFailedProperties() {
        return this.failedProperties;
    }
}
