package javax.net.ssl;

/**
 * Hay algo mal con una clave.
 *
 * <p>Del lado propio, no del par: la clave no se pudo usar, esta mal formada, o no corresponde al
 * certificado que la acompana. Un problema con la clave del par llega como
 * {@link SSLPeerUnverifiedException}.
 */
public class SSLKeyException extends SSLException {

    private static final long serialVersionUID = -8071664081190424597L;

    /** Con un mensaje. */
    public SSLKeyException(String reason) {
        super(reason);
    }

    /** Con un mensaje y la causa de fondo. */
    public SSLKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
