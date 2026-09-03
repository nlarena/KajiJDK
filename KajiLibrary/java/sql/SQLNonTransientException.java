package java.sql;

/**
 * KajiLibrary's java.sql.SQLNonTransientException -- fallo que **no** se arregla reintentando.
 *
 * <p>La division entre esta y `SQLTransientException` es lo unico que aportan: le dicen a quien
 * atrapa si tiene sentido volver a intentar. Un error de sintaxis no mejora por reintentarse; un
 * bloqueo momentaneo si. Sin esta distincion, cada capa de reintento tendria que mirar codigos de
 * proveedor para decidir.
 */
public class SQLNonTransientException extends SQLException {

    public SQLNonTransientException() {
        super();
    }

    public SQLNonTransientException(String reason) {
        super(reason);
    }

    public SQLNonTransientException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public SQLNonTransientException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public SQLNonTransientException(Throwable cause) {
        super(cause);
    }

    public SQLNonTransientException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLNonTransientException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    public SQLNonTransientException(String reason, String SQLState, int vendorCode,
            Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
