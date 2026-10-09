package java.net.http;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A WebSocket connection: bidirectional, message-based, over the same HTTP connection.
 *
 * <h2>Backpressure, which is what there is to understand about this API</h2>
 *
 * <p>The client does <strong>not</strong> deliver messages until they are requested. {@link
 * #request(long)} is how they are requested, and until more is requested, nothing arrives. It is
 * the opposite of the "listen for events" reflex.
 *
 * <p>It sounds awkward and solves a real problem: a server sending faster than the program
 * processes would grow a queue without limit until memory ran out. With this, whoever cannot keep
 * up simply does not ask, and the pressure travels back through the network to the sender.
 *
 * <p>The {@link CompletionStage} each {@link Listener} method returns says when the WebSocket may
 * reclaim that message's data; {@code null} means at once. It does not request the next message.
 * This note said that when the stage completes the client requests one more by itself; what
 * requests one more in the JDK is the default method bodies, which call {@code request(1)} — and
 * here only {@link Listener#onOpen}'s does (see {@link Listener}).
 *
 * <h2>Messages come in parts</h2>
 *
 * <p>{@link Listener#onText} receives a {@code boolean last}: a message can arrive in several
 * parts, and only the last one closes it. Putting them together is the listener's job, and the same
 * parameter appears on the {@link #sendText} side so messages can be sent that way.
 *
 * <h2>In this VM</h2>
 *
 * <p>The interface is whole, apart from the listener defaults above; what is missing is an
 * implementation, because there is no HTTP client to do the handshake. See {@link HttpClient}.
 *
 * @since 11
 */
public interface WebSocket {

    /**
     * The normal closure code, {@code 1000}.
     *
     * <p>The codes are the specification's, not the JDK's: closing with another number tells the
     * other side something different.
     */
    int NORMAL_CLOSURE = 1000;

    /**
     * Sends text, or a part of it.
     *
     * @param last whether this part closes the message
     */
    CompletableFuture<WebSocket> sendText(CharSequence data, boolean last);

    /** Sends binary data, or a part of it. */
    CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last);

    /** Sends a ping; the other side must answer with a pong. */
    CompletableFuture<WebSocket> sendPing(ByteBuffer message);

    /**
     * Sends a pong.
     *
     * <p>It is not needed in answer to a ping —the client already does that by itself—: this is for
     * the <em>unsolicited</em> pong, which serves as a one-way heartbeat.
     */
    CompletableFuture<WebSocket> sendPong(ByteBuffer message);

    /**
     * Starts closing.
     *
     * <p>In order: the other side answers with its own close and only then does the connection end.
     * What was already in flight is delivered.
     *
     * @param statusCode {@link #NORMAL_CLOSURE} or another from the specification
     */
    CompletableFuture<WebSocket> sendClose(int statusCode, String reason);

    /**
     * Requests {@code n} more messages.
     *
     * <p>Without this nothing arrives; see the class note.
     */
    void request(long n);

    /** The agreed subprotocol, or the empty string if none was agreed. */
    String getSubprotocol();

    /** Whether nothing more can be sent. */
    boolean isOutputClosed();

    /** Whether nothing more will be received. */
    boolean isInputClosed();

    /**
     * Cuts off without an orderly close.
     *
     * <p>For when the normal close is not possible or not worth waiting for. Whatever was in flight
     * is lost.
     */
    void abort();

    /**
     * Builds a {@link WebSocket}.
     *
     * <p>It is obtained with {@link HttpClient#newWebSocketBuilder}: a WebSocket starts out as an
     * HTTP request, so it inherits the client's TLS, proxy and executor.
     */
    public interface Builder {

        /** Adds a header to the handshake. */
        Builder header(String name, String value);

        /** The timeout for completing the handshake. */
        Builder connectTimeout(Duration timeout);

        /**
         * The subprotocols to offer, in order of preference.
         *
         * <p>The server picks one or none; which one it picked is read with {@link
         * WebSocket#getSubprotocol}.
         */
        Builder subprotocols(String mostPreferred, String... lesserPreferred);

        /**
         * Connects.
         *
         * <p>The future fails with {@link WebSocketHandshakeException} if the server refused the
         * protocol switch — and that exception carries the HTTP response, which usually explains
         * why.
         */
        CompletableFuture<WebSocket> buildAsync(URI uri, Listener listener);
    }

    /**
     * Handles what arrives over the WebSocket.
     *
     * <p>Every method has a body: hardly anyone needs all seven. This note said the ones returning
     * a {@link CompletionStage} return {@code null}, meaning "done with this message, request the
     * next". {@code null} only says the data may be reclaimed at once, and nothing requests the
     * next message: in the JDK the defaults of {@code onText}, {@code onBinary}, {@code onPing} and
     * {@code onPong} call {@code request(1)} first, but in this tree they only return {@code null}.
     */
    public interface Listener {

        /**
         * The connection opened.
         *
         * <p>By default it requests one message. Whoever overrides it <strong>has to</strong> call
         * {@link WebSocket#request} or will never receive anything — the easiest mistake to make
         * with this API.
         */
        default void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        /** Text arrived, or a part of it. */
        default CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            return null;
        }

        /** Binary data arrived, or a part of it. */
        default CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            return null;
        }

        /** A ping arrived; the WebSocket answers it with a pong by itself. */
        default CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            return null;
        }

        /** A pong arrived. */
        default CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            return null;
        }

        /** The other side started closing. */
        default CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            return null;
        }

        /**
         * Something failed, and the connection is already closed.
         *
         * <p>There is nothing to retry on this WebSocket: when this is called, {@code onClose} will
         * not arrive.
         */
        default void onError(WebSocket webSocket, Throwable error) {
        }
    }
}
