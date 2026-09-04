package java.net.http;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Una conexion WebSocket: bidireccional, por mensajes, sobre la misma conexion HTTP.
 *
 * <h2>La contrapresion, que es lo que hay que entender de esta API</h2>
 *
 * <p>El cliente <strong>no</strong> entrega mensajes hasta que se los pidan. {@link #request(long)}
 * es como se piden, y hasta que no se pida nada mas, nada llega. Es lo contrario del reflejo de
 * "escuchar eventos".
 *
 * <p>Suena incomodo y resuelve un problema real: un servidor que manda mas rapido de lo que el
 * programa procesa haria crecer una cola sin limite hasta agotar la memoria. Con esto, el que no da
 * abasto simplemente no pide, y la presion vuelve por la red hasta el emisor.
 *
 * <p>El JDK lo hace comodo: cada metodo de {@link Listener} devuelve un {@link CompletionStage}, y
 * cuando ese se completa el cliente pide uno mas solo. Devolver {@code null} —el valor por omision—
 * significa "ya termine con este".
 *
 * <h2>Los mensajes vienen partidos</h2>
 *
 * <p>{@link Listener#onText} recibe un {@code boolean last}: un mensaje puede llegar en varias
 * partes, y solo la ultima lo cierra. Juntarlas es responsabilidad de quien escucha, y el mismo
 * parametro aparece del lado de {@link #sendText} para poder mandar asi.
 *
 * <h2>En esta VM</h2>
 *
 * <p>La interfaz esta entera; lo que no hay es implementacion, porque no hay cliente HTTP que haga
 * el handshake. Ver {@link HttpClient}.
 *
 * @since 11
 */
public interface WebSocket {

    /**
     * El codigo de cierre normal, {@code 1000}.
     *
     * <p>Los codigos son de la especificacion, no del JDK: cerrar con otro numero le dice algo
     * distinto al otro lado.
     */
    int NORMAL_CLOSURE = 1000;

    /**
     * Manda texto, o una parte.
     *
     * @param last si esta parte cierra el mensaje
     */
    CompletableFuture<WebSocket> sendText(CharSequence data, boolean last);

    /** Manda binario, o una parte. */
    CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last);

    /** Manda un ping; el otro lado debe contestar con un pong. */
    CompletableFuture<WebSocket> sendPing(ByteBuffer message);

    /**
     * Manda un pong.
     *
     * <p>No hace falta mandarlo en respuesta a un ping —el cliente ya lo hace solo—: esto es para
     * el pong <em>no solicitado</em>, que sirve como latido unidireccional.
     */
    CompletableFuture<WebSocket> sendPong(ByteBuffer message);

    /**
     * Empieza a cerrar.
     *
     * <p>Ordenado: el otro lado contesta con su propio cierre y recien ahi la conexion termina. Lo
     * que ya estaba en vuelo se entrega.
     *
     * @param statusCode {@link #NORMAL_CLOSURE} u otro de la especificacion
     */
    CompletableFuture<WebSocket> sendClose(int statusCode, String reason);

    /**
     * Pide {@code n} mensajes mas.
     *
     * <p>Sin esto no llega nada; ver la nota de la clase.
     */
    void request(long n);

    /** El subprotocolo acordado, o la cadena vacia si no se acordo ninguno. */
    String getSubprotocol();

    /** Si ya no se puede mandar. */
    boolean isOutputClosed();

    /** Si ya no se va a recibir. */
    boolean isInputClosed();

    /**
     * Corta sin cierre ordenado.
     *
     * <p>Para cuando el cierre normal no es posible o no vale la pena esperarlo. Lo que estuviera en
     * vuelo se pierde.
     */
    void abort();

    /**
     * Arma un {@link WebSocket}.
     *
     * <p>Se lo consigue con {@link HttpClient#newWebSocketBuilder}: el WebSocket empieza siendo un
     * pedido HTTP, asi que hereda del cliente el TLS, el proxy y el ejecutor.
     */
    public interface Builder {

        /** Agrega un encabezado al handshake. */
        Builder header(String name, String value);

        /** El plazo para completar el handshake. */
        Builder connectTimeout(Duration timeout);

        /**
         * Los subprotocolos a ofrecer, en orden de preferencia.
         *
         * <p>El servidor elige uno o ninguno; cual toco se lee con {@link WebSocket#getSubprotocol}.
         */
        Builder subprotocols(String mostPreferred, String... lesserPreferred);

        /**
         * Conecta.
         *
         * <p>El futuro falla con {@link WebSocketHandshakeException} si el servidor rechazo el
         * cambio de protocolo — y esa excepcion lleva adentro la respuesta HTTP, que suele explicar
         * por que.
         */
        CompletableFuture<WebSocket> buildAsync(URI uri, Listener listener);
    }

    /**
     * Atiende lo que llega por el WebSocket.
     *
     * <p>Todos los metodos tienen cuerpo: casi nadie necesita los siete, y los que devuelven
     * {@link CompletionStage} devuelven {@code null}, que significa "termine con este mensaje, pedi
     * el siguiente".
     */
    public interface Listener {

        /**
         * La conexion se abrio.
         *
         * <p>Por omision pide un mensaje. Quien lo sobrescriba <strong>tiene que</strong> llamar a
         * {@link WebSocket#request} o no va a recibir nada nunca — es el error mas facil de cometer
         * con esta API.
         */
        default void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        /** Llego texto, o una parte. */
        default CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            return null;
        }

        /** Llego binario, o una parte. */
        default CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            return null;
        }

        /** Llego un ping; el cliente ya contesto el pong. */
        default CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            return null;
        }

        /** Llego un pong. */
        default CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            return null;
        }

        /** El otro lado empezo a cerrar. */
        default CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            return null;
        }

        /**
         * Algo fallo, y la conexion ya esta cerrada.
         *
         * <p>No hay nada que reintentar sobre este WebSocket: cuando esto se llama, el
         * {@code onClose} no va a llegar.
         */
        default void onError(WebSocket webSocket, Throwable error) {
        }
    }
}
