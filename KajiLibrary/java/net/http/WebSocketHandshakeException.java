package java.net.http;

import java.io.IOException;

/**
 * El servidor rechazo el cambio a WebSocket.
 *
 * <p>Lo notable es {@link #getResponse}: <strong>lleva la respuesta HTTP adentro</strong>. Un
 * handshake de WebSocket empieza siendo un pedido HTTP normal, y cuando falla el servidor contesta
 * con un codigo y un cuerpo que suelen explicar por que — un {@code 401}, un {@code 404}, un
 * subprotocolo no soportado. Sin la respuesta, todo eso se perderia y quedaria "no se pudo".
 *
 * @since 11
 */
public final class WebSocketHandshakeException extends IOException {

    private static final long serialVersionUID = 1L;

    private final transient HttpResponse<?> response;

    /** Con la respuesta que dio el servidor. */
    public WebSocketHandshakeException(HttpResponse<?> response) {
        this.response = response;
    }

    /** La respuesta HTTP del rechazo. */
    public HttpResponse<?> getResponse() {
        return this.response;
    }

    /**
     * Fija la causa y devuelve <strong>esta</strong> clase, no {@link Throwable}.
     *
     * <p>Es un tipo de retorno covariante y sirve para encadenar sin castear:
     * {@code throw new WebSocketHandshakeException(r).initCause(e);} compila porque el tipo que
     * vuelve ya es el correcto.
     */
    public WebSocketHandshakeException initCause(Throwable cause) {
        super.initCause(cause);
        return this;
    }
}
