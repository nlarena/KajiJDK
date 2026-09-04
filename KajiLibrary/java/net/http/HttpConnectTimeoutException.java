package java.net.http;

/**
 * Se vencio el plazo <strong>antes de conectar</strong>.
 *
 * <h2>Por que merece su propio tipo</h2>
 *
 * <p>Porque distingue "no llegue" de "llegue y tardo". Si la conexion nunca se establecio, el
 * servidor <strong>no vio el pedido</strong> — y entonces reintentar es seguro incluso para un
 * {@code POST}, que no lo seria si la respuesta se hubiera perdido despues de haberlo procesado.
 *
 * <p>Es la unica forma que da esta API de saber eso, y por eso vale un tipo y no un campo.
 *
 * @since 11
 */
public class HttpConnectTimeoutException extends HttpTimeoutException {

    private static final long serialVersionUID = 321L + 11L;

    /** Con un mensaje. */
    public HttpConnectTimeoutException(String message) {
        super(message);
    }
}
