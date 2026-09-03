package java.sql;

/**
 * KajiLibrary's java.sql.SQLException -- lo que falla al hablar con una base de datos.
 *
 * <p>Lleva **tres** datos y no uno, y esa es toda su forma: el mensaje, un `SQLState` de cinco
 * caracteres --un codigo estandarizado, el mismo para todas las bases-- y un codigo de error
 * **propio del proveedor**. Los tres existen porque el estandar no alcanza: el `SQLState` dice
 * "violacion de restriccion" y el codigo del proveedor dice *cual*.
 *
 * <p>Y son **encadenables** entre si por {@link #setNextException}: una sola operacion puede fallar
 * por varias razones a la vez --un lote de inserciones-- y aplastarlas en una sola perderia todas
 * menos la primera. La cadena es distinta de la de `getCause`, que dice "por que paso esto"; esta
 * dice "y ademas paso esto otro".
 */
public class SQLException extends Exception implements Iterable<Throwable> {

    private final String sqlState;
    private final int vendorCode;
    private volatile SQLException next;

    public SQLException() {
        this(null, null, 0, null);
    }

    public SQLException(String reason) {
        this(reason, null, 0, null);
    }

    public SQLException(String reason, String sqlState) {
        this(reason, sqlState, 0, null);
    }

    public SQLException(String reason, String sqlState, int vendorCode) {
        this(reason, sqlState, vendorCode, null);
    }

    public SQLException(Throwable cause) {
        this(null, null, 0, cause);
    }

    public SQLException(String reason, Throwable cause) {
        this(reason, null, 0, cause);
    }

    public SQLException(String reason, String sqlState, Throwable cause) {
        this(reason, sqlState, 0, cause);
    }

    public SQLException(String reason, String sqlState, int vendorCode, Throwable cause) {
        super(reason, cause);
        this.sqlState = sqlState;
        this.vendorCode = vendorCode;
    }

    /** El codigo estandar de cinco caracteres, o `null` si el proveedor no lo dio. */
    public String getSQLState() {
        return this.sqlState;
    }

    /** El codigo de error **del proveedor**; cero si no dio ninguno. */
    public int getErrorCode() {
        return this.vendorCode;
    }

    /** La siguiente excepcion de la cadena, o `null`. */
    public SQLException getNextException() {
        return this.next;
    }

    /**
     * Agrega `ex` **al final** de la cadena.
     *
     * <p>Al final y no al principio: el orden de la cadena es el orden en que los fallos ocurrieron,
     * y ponerlos al reves haria que el primer error reportado fuera el ultimo que paso.
     */
    public void setNextException(SQLException ex) {
        SQLException actual = this;
        synchronized (this) {
            while (actual.next != null) {
                actual = actual.next;
            }
            actual.next = ex;
        }
    }

    /**
     * Recorre esta excepcion, su causa, y las que le siguen en la cadena.
     *
     * <p>Recorre **las dos** dimensiones --la cadena de `next` y la de `getCause`-- porque las dos
     * llevan informacion distinta y quien diagnostica quiere ver todo.
     */
    public java.util.Iterator<Throwable> iterator() {
        java.util.ArrayList<Throwable> todas = new java.util.ArrayList<Throwable>();
        SQLException e = this;
        while (e != null) {
            todas.add(e);
            Throwable causa = e.getCause();
            while (causa != null) {
                todas.add(causa);
                causa = causa.getCause();
            }
            e = e.next;
        }
        return todas.iterator();
    }
}
