package javax.net.ssl;

/**
 * Un error en el protocolo mismo.
 *
 * <p>Lo que llego no es SSL/TLS valido: un mensaje mal formado, un campo fuera de rango, un orden
 * imposible. Distinta de {@link SSLHandshakeException}, donde los mensajes eran correctos y lo que
 * fallo fue el acuerdo.
 */
public class SSLProtocolException extends SSLException {

    private static final long serialVersionUID = 5445067063799134928L;

    /** Con un mensaje. */
    public SSLProtocolException(String reason) {
        super(reason);
    }

    /** Con un mensaje y la causa de fondo. */
    public SSLProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
