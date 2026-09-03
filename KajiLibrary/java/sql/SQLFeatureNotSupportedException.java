package java.sql;

/**
 * KajiLibrary's java.sql.SQLFeatureNotSupportedException -- el driver no implementa eso.
 *
 * <p>Es la excepcion que hace vivible una API tan grande como JDBC: ningun driver implementa las
 * ochocientas y pico de operaciones, y esta es la manera declarada de decir "esta no". Que herede de
 * {@link SQLNonTransientException} es la parte util -- avisa de entrada que reintentar no va a
 * cambiar nada.
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
