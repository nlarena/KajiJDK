package java.sql;

/**
 * KajiLibrary's java.sql.SQLClientInfoException -- a failure setting client properties.
 *
 * <p>It is the only JDBC exception that carries a **map** instead of a single reason, and for a
 * concrete reason: `setClientInfo` receives several properties together and can fail on some of
 * them. A single reason would force throwing at the first one that fails and saying nothing about
 * the rest.
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

    /** Which property failed and why. */
    public java.util.Map<String, ClientInfoStatus> getFailedProperties() {
        return this.failedProperties;
    }
}
