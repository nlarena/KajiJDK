package java.sql;

/**
 * KajiLibrary's java.sql.SQLFeatureNotSupportedException -- the driver does not implement that.
 *
 * <p>It is the exception that makes an API as large as JDBC livable: no driver implements all of
 * its hundreds of operations, and this is the declared way of saying "not this one". That it
 * inherits from {@link SQLNonTransientException} is the useful part -- it warns up front that
 * retrying will change nothing.
 */
public class SQLFeatureNotSupportedException extends SQLNonTransientException {

    public SQLFeatureNotSupportedException() {
        super();
    }

    public SQLFeatureNotSupportedException(String reason) {
        super(reason);
    }

    public SQLFeatureNotSupportedException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLFeatureNotSupportedException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLFeatureNotSupportedException(Throwable cause) {
        super(cause);
    }

    public SQLFeatureNotSupportedException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLFeatureNotSupportedException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLFeatureNotSupportedException(String reason, String SQLState, int vendorCode,
            Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
