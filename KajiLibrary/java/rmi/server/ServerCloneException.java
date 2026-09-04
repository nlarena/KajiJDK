package java.rmi.server;

/**
 * Fallo al clonar un objeto remoto exportado.
 *
 * <p>Clonar uno de estos no es copiar campos: la copia tiene que <strong>exportarse tambien</strong>,
 * porque un objeto remoto sin exportar no es alcanzable. Esa exportacion es la que puede fallar, y
 * por eso existe una excepcion de clonacion propia.
 *
 * <p>El campo publico {@link #detail} y los dos metodos que lo usan son anteriores a que
 * {@link Throwable} tuviera causas. Se conservan por compatibilidad; {@link #getCause} devuelve lo
 * mismo.
 */
public class ServerCloneException extends CloneNotSupportedException {

    private static final long serialVersionUID = 6617456357664815945L;

    /** La causa, en la forma vieja. */
    public Exception detail;

    /** Con un mensaje. */
    public ServerCloneException(String s) {
        super(s);
    }

    /** Con un mensaje y la causa. */
    public ServerCloneException(String s, Exception cause) {
        super(s);
        this.detail = cause;
    }

    /** El mensaje, con el de la causa pegado si la hay. */
    public String getMessage() {
        if (this.detail == null) {
            return super.getMessage();
        }
        return super.getMessage() + "; nested exception is: "
                + this.detail.toString();
    }

    /** La causa; es {@link #detail}. */
    public Throwable getCause() {
        return this.detail;
    }
}
