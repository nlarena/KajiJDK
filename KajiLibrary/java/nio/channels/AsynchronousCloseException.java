package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.AsynchronousCloseException — El canal lo cerro **otro hilo** mientras esta operacion estaba en curso.
 *
 * <p>Extiende `ClosedChannelException` y agrega la parte que importa para diagnosticar: el canal
 * no estaba cerrado cuando la operacion empezo. Quien la reciba sabe que no se equivoco de orden;
 * le cerraron el canal debajo.
 */
public class AsynchronousCloseException extends ClosedChannelException {

    private static final long serialVersionUID = 1000000003L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public AsynchronousCloseException() {
        super();
    }
}
