package java.rmi.server;

/**
 * Se pidio {@link RemoteServer#getClientHost} fuera de la atencion de una llamada remota.
 *
 * <p>No es un error del servidor sino una pregunta mal ubicada: "quien es el cliente" solo tiene
 * respuesta mientras se esta atendiendo a uno. Preguntarlo desde otro hilo, o antes de que llegue
 * la llamada, no tiene sentido — y devolver {@code null} lo habria disimulado.
 */
public class ServerNotActiveException extends Exception {

    private static final long serialVersionUID = 4687940720827538231L;

    /** Sin detalle. */
    public ServerNotActiveException() {
        super();
    }

    /** Con un mensaje. */
    public ServerNotActiveException(String s) {
        super(s);
    }
}
