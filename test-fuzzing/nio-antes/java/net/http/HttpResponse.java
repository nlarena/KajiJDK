package java.net.http;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.net.ssl.SSLSession;

/**
 * An HTTP response, with the body already converted to the requested type.
 *
 * <h2>The type parameter, which is the first thing that surprises</h2>
 *
 * <p>{@code HttpResponse<T>}, where {@code T} is chosen by <strong>whoever makes the
 * request</strong>, with the {@link BodyHandler} it passes to the client. Asking for {@code
 * BodyHandlers.ofString()} gives an {@code HttpResponse<String>}; {@code ofFile(path)} gives an
 * {@code HttpResponse<Path>}.
 *
 * <p>That is not sugar. The conversion happens <strong>while the body arrives</strong>, not after:
 * a body written to a file is never whole in memory, and one that is discarded is stored nowhere.
 * An {@code HttpResponse} with a {@code byte[]} inside could do none of that.
 *
 * <h2>The three-piece chain</h2>
 *
 * <ul>
 * <li>{@link BodyHandler} — looks at the {@link ResponseInfo} (status, headers, version) and
 *     <strong>decides</strong> how to read the body. It is what allows discarding a 404's body and
 *     keeping a 200's without two requests;</li>
 * <li>{@link BodySubscriber} — the one that actually reads it, with backpressure;</li>
 * <li>{@link BodyHandlers} and {@link BodySubscribers} — the ones the JDK provides, so they need
 *     not be written.</li>
 * </ul>
 *
 * @param <T> the type of the converted body
 * @since 11
 */
public interface HttpResponse<T> {

    /** The HTTP status code. */
    int statusCode();

    /**
     * A label for the connection it came through, for diagnostics.
     *
     * <p>It came with a body for compatibility, and empty by default: not every implementation has
     * a connection identifier to offer.
     *
     * @since 25
     */
    default Optional<String> connectionLabel() {
        return Optional.empty();
    }

    /** The request that produced it; it may not be the original if there were redirects. */
    HttpRequest request();

    /**
     * The previous response, if this one came after a redirect.
     *
     * <p>It is a chain: each link points to the previous one. It shows the path the request took,
     * which would otherwise be invisible.
     */
    Optional<HttpResponse<T>> previousResponse();

    /** The headers. */
    HttpHeaders headers();

    /** The body, already converted. */
    T body();

    /** The TLS session, if it came over HTTPS. */
    Optional<SSLSession> sslSession();

    /** The URI it was finally obtained from, following redirects. */
    URI uri();

    /** The version spoken. */
    HttpClient.Version version();

    /**
     * Decides how to read the body, looking at what is already known of the response.
     *
     * <p>It is consulted <strong>once</strong>, when the headers have arrived and before the body.
     * That moment is the whole point: it is what allows choosing by status or content type without
     * having downloaded anything yet.
     */
    @FunctionalInterface
    public interface BodyHandler<T> {

        /** The subscriber for this response. */
        BodySubscriber<T> apply(ResponseInfo responseInfo);
    }

    /** What a {@link BodyHandler} knows of the response before the body arrives. */
    public interface ResponseInfo {

        /** The HTTP status code. */
        int statusCode();

        /** The headers. */
        HttpHeaders headers();

        /** The version. */
        HttpClient.Version version();
    }

    /**
     * Reads the body and produces the result.
     *
     * <p>It is a {@link Flow.Subscriber} of <strong>lists</strong> of {@link ByteBuffer} and not of
     * single buffers: the network delivers in blocks, and grouping them avoids one notification per
     * buffer.
     *
     * <p>{@link #getBody} returns a {@link CompletionStage} that completes when the result is
     * available. It may complete <em>before</em> the whole body has been read —a streaming
     * subscriber, such as the one {@link BodySubscribers#ofInputStream} returns, completes it with
     * the stream right away— and that is the difference from waiting for {@code onComplete}. (This
     * javadoc gave as the example a reader that only wants the headers; the headers are known
     * before any subscriber exists.)
     */
    public interface BodySubscriber<T> extends Flow.Subscriber<List<ByteBuffer>> {

        /** The result, when it is ready. */
        CompletionStage<T> getBody();
    }

    /**
     * Handles the responses the server sends without being asked.
     *
     * <p>It is HTTP/2's: a server serving a page can push right away the stylesheets it knows will
     * be requested. Without this handler the client rejects them, which is the right default —
     * accepting unrequested content has to be an explicit decision.
     */
    public interface PushPromiseHandler<T> {

        /** A promise arrives; accepting it is calling the {@code acceptor} with a body handler. */
        void applyPushPromise(HttpRequest initiatingRequest, HttpRequest pushPromiseRequest,
                Function<BodyHandler<T>, CompletableFuture<HttpResponse<T>>> acceptor);

        /** The handler that accepts all of them and gathers them in that map. */
        static <T> PushPromiseHandler<T> of(
                Function<HttpRequest, BodyHandler<T>> pushPromiseHandler,
                ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<T>>> pushPromisesMap) {
            throw new UnsupportedOperationException(
                    "this VM has no HTTP client implementation; see HttpClient");
        }
    }

    /**
     * The {@link BodyHandler}s the JDK provides.
     *
     * <p>In this VM they refuse: there is no HTTP client implementation. See {@link HttpClient}.
     */
    public static class BodyHandlers {

        private BodyHandlers() {
        }

        private static <T> BodyHandler<T> refuse() {
            throw new UnsupportedOperationException(
                    "this VM has no HTTP client implementation; see HttpClient");
        }

        /** Passes the blocks to that subscriber; the body is {@code null}. */
        public static BodyHandler<Void> fromSubscriber(
                Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            return refuse();
        }

        /** The same, extracting the result from the subscriber with that function. */
        public static <S extends Flow.Subscriber<? super List<ByteBuffer>>, T> BodyHandler<T>
                fromSubscriber(S subscriber, Function<? super S, ? extends T> finisher) {
            return refuse();
        }

        /** Passes the body line by line to that subscriber. */
        public static BodyHandler<Void> fromLineSubscriber(
                Flow.Subscriber<? super String> subscriber) {
            return refuse();
        }

        /** The same, with its own extractor and line separator. */
        public static <S extends Flow.Subscriber<? super String>, T> BodyHandler<T>
                fromLineSubscriber(S subscriber, Function<? super S, ? extends T> finisher,
                        String lineSeparator) {
            return refuse();
        }

        /**
         * Discards the body. It does not ignore it: it reads it and throws it away, which is what
         * frees the connection.
         */
        public static BodyHandler<Void> discarding() {
            return refuse();
        }

        /** Discards the body and returns that fixed value. */
        public static <U> BodyHandler<U> replacing(U value) {
            return refuse();
        }

        /** The body as a string, with that charset. */
        public static BodyHandler<String> ofString(Charset charset) {
            return refuse();
        }

        /** The body to that file, with those open options. */
        public static BodyHandler<Path> ofFile(Path file, OpenOption... openOptions) {
            return refuse();
        }

        /** The body to that file. */
        public static BodyHandler<Path> ofFile(Path file) {
            return refuse();
        }

        /**
         * The body to a file inside that directory, with the name the server gives.
         *
         * <p>The name comes from the {@code Content-Disposition} header, that is, <strong>from the
         * other side</strong>. The client validates it so it cannot escape the directory, and even
         * so it is worth knowing that the server picks the name.
         */
        public static BodyHandler<Path> ofFileDownload(Path directory, OpenOption... openOptions) {
            return refuse();
        }

        /**
         * The body as a stream to read later.
         *
         * <p>It has to be closed or read to the end: until then, the connection stays taken.
         */
        public static BodyHandler<InputStream> ofInputStream() {
            return refuse();
        }

        /** The body as a stream of lines. */
        public static BodyHandler<Stream<String>> ofLines() {
            return refuse();
        }

        /** Passes the blocks to that consumer; the empty one marks the end. */
        public static BodyHandler<Void> ofByteArrayConsumer(
                Consumer<Optional<byte[]>> consumer) {
            return refuse();
        }

        /** The body as a byte array, whole in memory. */
        public static BodyHandler<byte[]> ofByteArray() {
            return refuse();
        }

        /** The body as a string, in UTF-8 or whatever {@code Content-Type} says. */
        public static BodyHandler<String> ofString() {
            return refuse();
        }

        /** The body as a publisher, to consume with backpressure of one's own. */
        public static BodyHandler<Flow.Publisher<List<ByteBuffer>>> ofPublisher() {
            return refuse();
        }

        /** Groups the blocks up to that size before delivering them. */
        public static <T> BodyHandler<T> buffering(BodyHandler<T> downstream, int bufferSize) {
            return refuse();
        }

        /**
         * Cuts off if the body goes past that size.
         *
         * <p>It is the defence against a server that sends more than one can keep, by mistake or on
         * purpose.
         */
        public static <T> BodyHandler<T> limiting(BodyHandler<T> downstream, long capacity) {
            return refuse();
        }
    }

    /**
     * The {@link BodySubscriber}s the JDK provides.
     *
     * <p>They are the counterpart of {@link BodyHandlers}: those <em>choose</em> by looking at the
     * response, these <em>read</em>. They are used directly when writing a handler of one's own.
     *
     * <p>In this VM they refuse; see {@link HttpClient}.
     */
    public static class BodySubscribers {

        private BodySubscribers() {
        }

        private static <T> BodySubscriber<T> refuse() {
            throw new UnsupportedOperationException(
                    "this VM has no HTTP client implementation; see HttpClient");
        }

        /** Forwards the blocks to that subscriber. */
        public static BodySubscriber<Void> fromSubscriber(
                Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            return refuse();
        }

        /** The same, extracting the result. */
        public static <S extends Flow.Subscriber<? super List<ByteBuffer>>, T> BodySubscriber<T>
                fromSubscriber(S subscriber, Function<? super S, ? extends T> finisher) {
            return refuse();
        }

        /** Forwards line by line. */
        public static BodySubscriber<Void> fromLineSubscriber(
                Flow.Subscriber<? super String> subscriber) {
            return refuse();
        }

        /** The same, with its own extractor, charset and line separator. */
        public static <S extends Flow.Subscriber<? super String>, T> BodySubscriber<T>
                fromLineSubscriber(S subscriber, Function<? super S, ? extends T> finisher,
                        Charset charset, String lineSeparator) {
            return refuse();
        }

        /** Gathers the body into a string. */
        public static BodySubscriber<String> ofString(Charset charset) {
            return refuse();
        }

        /** Gathers the body into an array. */
        public static BodySubscriber<byte[]> ofByteArray() {
            return refuse();
        }

        /** Writes the body to that file. */
        public static BodySubscriber<Path> ofFile(Path file, OpenOption... openOptions) {
            return refuse();
        }

        /** Writes the body to that file. */
        public static BodySubscriber<Path> ofFile(Path file) {
            return refuse();
        }

        /** Passes the blocks to that consumer. */
        public static BodySubscriber<Void> ofByteArrayConsumer(
                Consumer<Optional<byte[]>> consumer) {
            return refuse();
        }

        /** The body as a stream to read later. */
        public static BodySubscriber<InputStream> ofInputStream() {
            return refuse();
        }

        /** The body as a stream of lines. */
        public static BodySubscriber<Stream<String>> ofLines(Charset charset) {
            return refuse();
        }

        /** The body as a publisher. */
        public static BodySubscriber<Flow.Publisher<List<ByteBuffer>>> ofPublisher() {
            return refuse();
        }

        /** Discards the body and returns that value. */
        public static <U> BodySubscriber<U> replacing(U value) {
            return refuse();
        }

        /** Discards the body. */
        public static BodySubscriber<Void> discarding() {
            return refuse();
        }

        /** Groups before delivering. */
        public static <T> BodySubscriber<T> buffering(BodySubscriber<T> downstream,
                int bufferSize) {
            return refuse();
        }

        /**
         * Transforms the result of another subscriber.
         *
         * <p>The function runs once the upstream result is available. This javadoc said it can
         * therefore block without holding up the reading; the JDK warns against that —a blocking
         * mapper can starve the client's executor— and suggests mapping to a {@code Supplier} and
         * blocking in the caller's thread.
         */
        public static <T, U> BodySubscriber<U> mapping(BodySubscriber<T> upstream,
                Function<? super T, ? extends U> mapper) {
            return refuse();
        }

        /** Cuts off if the body goes past that size. */
        public static <T> BodySubscriber<T> limiting(BodySubscriber<T> downstream, long capacity) {
            return refuse();
        }
    }
}
