package java.net.http;

import java.io.IOException;

/**
 * Se vencio el plazo de un pedido HTTP.
 *
 * <p>Es una {@link IOException} porque para quien la recibe es eso: la operacion no se completo. Lo
 * que agrega sobre una comun es que <strong>el plazo lo puso quien llamo</strong> — no es que la red
 * fallara, es que tardo mas de lo que se le concedio. La diferencia importa al decidir si reintentar.
 *
 * <p>Su subclase {@link HttpConnectTimeoutException} distingue el caso mas util: vencerse
 * <em>conectando</em>.
 *
 * @since 11
 */
public class HttpTimeoutException extends IOException {

    private static final long serialVersionUID = 981344271622632951L;

    /** Con un mensaje. */
    public HttpTimeoutException(String message) {
        super(message);
    }
}
