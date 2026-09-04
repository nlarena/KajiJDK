package javax.sql.rowset;

import java.sql.SQLException;

/**
 * Un aviso de un {@code RowSet}: algo que conviene saber y que no impidio seguir.
 *
 * <h2>Por que es una excepcion que nadie lanza</h2>
 *
 * <p>Porque hereda de {@link SQLException} para reusar su forma —mensaje, estado SQL, codigo de
 * error— pero se <strong>devuelve</strong>, no se lanza. Si se lanzara, la operacion se cortaria, y
 * la definicion de un aviso es justamente que no la corto.
 *
 * <h2>La cadena</h2>
 *
 * <p>Una operacion puede generar varios avisos y estos se encadenan con
 * {@link #setNextWarning}. El que consulta obtiene el primero y recorre con
 * {@link #getNextWarning} hasta que devuelva {@code null}.
 *
 * <p>Es la misma forma que usa {@code SQLWarning}, y existe aparte porque un aviso de
 * {@code RowSet} puede venir de la capa desconectada —una fila que no se pudo sincronizar— y no de
 * la base.
 *
 * @since 1.5
 */
public class RowSetWarning extends SQLException {

    private static final long serialVersionUID = 6242892457637296120L;

    private RowSetWarning rwarning;

    /**
     * Con un mensaje.
     *
     * @param reason el mensaje
     */
    public RowSetWarning(String reason) {
        super(reason);
    }

    /** Sin detalle. */
    public RowSetWarning() {
        super();
    }

    /**
     * Con mensaje y estado SQL.
     *
     * @param reason el mensaje
     * @param SQLState el estado SQL
     */
    public RowSetWarning(String reason, String SQLState) {
        super(reason, SQLState);
    }

    /**
     * Con mensaje, estado SQL y codigo de error.
     *
     * @param reason el mensaje
     * @param SQLState el estado SQL
     * @param vendorCode el codigo del fabricante
     */
    public RowSetWarning(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    /**
     * El aviso siguiente de la cadena.
     *
     * @return el siguiente, o {@code null} si este es el ultimo
     */
    public RowSetWarning getNextWarning() {
        return rwarning;
    }

    /**
     * Encadena otro aviso a continuacion de este.
     *
     * @param warning el aviso siguiente
     */
    public void setNextWarning(RowSetWarning warning) {
        rwarning = warning;
    }
}
