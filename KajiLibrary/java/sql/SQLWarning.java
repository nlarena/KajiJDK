package java.sql;

/**
 * KajiLibrary's java.sql.SQLWarning -- algo que la base quiso decir sin que la operacion fallara.
 *
 * <p>Que sea una **excepcion que no se lanza** suena raro y es deliberado: hereda el mensaje, el
 * `SQLState` y el encadenamiento de {@link SQLException}, que es exactamente lo que hace falta para
 * reportar un aviso, y a cambio evita un segundo tipo con la misma forma. Se la recoge preguntando,
 * no atrapando.
 */
public class SQLWarning extends SQLException {

    public SQLWarning() {
        super();
    }

    public SQLWarning(String reason) {
        super(reason);
    }

    public SQLWarning(String reason, String sqlState) {
        super(reason, sqlState);
    }

    public SQLWarning(String reason, String sqlState, int vendorCode) {
        super(reason, sqlState, vendorCode);
    }

    public SQLWarning(Throwable cause) {
        super(cause);
    }

    public SQLWarning(String reason, Throwable cause) {
        super(reason, cause);
    }

    public SQLWarning(String reason, String sqlState, Throwable cause) {
        super(reason, sqlState, cause);
    }

    public SQLWarning(String reason, String sqlState, int vendorCode, Throwable cause) {
        super(reason, sqlState, vendorCode, cause);
    }

    /** El siguiente aviso de la cadena, o `null`. */
    public SQLWarning getNextWarning() {
        SQLException siguiente = this.getNextException();
        if (siguiente == null) {
            return null;
        }
        if (siguiente instanceof SQLWarning) {
            return (SQLWarning) siguiente;
        }
        throw new Error("la cadena de avisos tiene una excepcion que no es un aviso");
    }

    /** Agrega `w` al final de la cadena de avisos. */
    public void setNextWarning(SQLWarning w) {
        this.setNextException(w);
    }
}
