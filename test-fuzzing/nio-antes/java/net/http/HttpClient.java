package java.net.http;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * The JDK's HTTP client: HTTP/1.1, HTTP/2 and WebSocket, with a synchronous and an asynchronous
 * API.
 *
 * <h2>It is a heavy object, and that changes how it is used</h2>
 *
 * <p>A client carries the connection pool, the thread pool, the TLS context and the session cache.
 * Creating one per request —the natural reflex coming from {@code URLConnection}— throws all of
 * that away every time and redoes the full handshake on every call.
 *
 * <p>The right thing is <strong>one per application</strong>, or one per distinct configuration,
 * shared between threads: it is immutable and safe for that.
 *
 * <h2>The two modes</h2>
 *
 * <p>{@link #send} blocks; {@link #sendAsync} returns a {@link CompletableFuture}. They are not two
 * implementations: the synchronous one is written on top of the other. That is why the client needs
 * an {@link Executor} even for the blocking mode.
 *
 * <h2>Closing, which came late</h2>
 *
 * <p>Until Java 21 there was no way to close it explicitly. This note said a client kept its
 * threads alive until the collector reached it, and that since the threads reference it that might
 * never happen. The JDK documents the opposite: its implementation releases the resources once the
 * instance is no longer strongly reachable and every operation started on it has completed. Since
 * 21 it is {@link AutoCloseable}, with the usual distinction between {@link #shutdown} —accepts no
 * new requests, finishes the ones in flight— and {@link #shutdownNow}, which cuts them off.
 *
 * <h2>In this VM</h2>
 *
 * <p>Both factories refuse. Implementing this would mean writing a whole HTTP/2 client —frames,
 * multiplexing, HPACK— over TLS that this VM has no provider to negotiate either. The API is
 * complete for whoever compiles against it, and what is missing is an implementation, not a
 * signature.
 *
 * <p>The shutdown methods are the exception: the JDK's base class gives them working defaults
 * —{@code shutdown} does nothing, {@code awaitTermination} returns {@code true}, {@code close}
 * waits on those— and here they throw.
 *
 * @since 11
 */
public abstract class HttpClient implements AutoCloseable {

    /** For implementations. */
    protected HttpClient() {
    }

    /**
     * A client with the default configuration.
     *
     * @throws UnsupportedOperationException in this VM — see the class note
     */
    public static HttpClient newHttpClient() {
        return newBuilder().build();
    }

    /**
     * A client builder.
     *
     * @throws UnsupportedOperationException in this VM
     */
    public static Builder newBuilder() {
        throw new UnsupportedOperationException(
                "this VM has no HTTP client implementation");
    }

    /** The cookie handler, if one was configured. */
    public abstract Optional<CookieHandler> cookieHandler();

    /**
     * The timeout for <strong>connecting</strong>, if configured.
     *
     * <p>Different from the {@link HttpRequest#timeout} one, which is for the whole request.
     * Running out while connecting gives {@link HttpConnectTimeoutException}, and that distinction
     * decides whether retrying is safe.
     */
    public abstract Optional<Duration> connectTimeout();

    /** What it does with redirects. */
    public abstract Redirect followRedirects();

    /** The proxy selector, if configured. */
    public abstract Optional<ProxySelector> proxy();

    /** The TLS context. */
    public abstract SSLContext sslContext();

    /** The TLS parameters. */
    public abstract SSLParameters sslParameters();

    /** The authenticator, if configured. */
    public abstract Optional<Authenticator> authenticator();

    /** The preferred version. It is a preference: HTTP/2 is negotiated and can fall back to 1.1. */
    public abstract Version version();

    /** The executor, if a custom one was configured. */
    public abstract Optional<Executor> executor();

    /**
     * Sends the request and waits for the response.
     *
     * @throws IOException if the network fails
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public abstract <T> HttpResponse<T> send(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException;

    /**
     * Sends the request without waiting.
     *
     * <p>The future completes when the headers have arrived <strong>and</strong> the body handler's
     * result is available. It fails with the exception inside rather than throwing it.
     */
    public abstract <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler);

    /** The same, also handling the promises the server pushes. */
    public abstract <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler);

    /**
     * A WebSocket builder.
     *
     * @throws UnsupportedOperationException if the client does not support it
     */
    public WebSocket.Builder newWebSocketBuilder() {
        throw new UnsupportedOperationException("this client does not support WebSocket");
    }

    /**
     * Stops accepting new requests; those in flight finish.
     *
     * @since 21
     */
    public void shutdown() {
        throw new UnsupportedOperationException("this client does not support orderly shutdown");
    }

    /**
     * Waits up to {@code duration} for the shutdown to finish.
     *
     * @return {@code true} if it finished, {@code false} if the time ran out
     * @since 21
     */
    public boolean awaitTermination(Duration duration) throws InterruptedException {
        throw new UnsupportedOperationException("this client does not support orderly shutdown");
    }

    /**
     * Whether it has finished shutting down.
     *
     * @since 21
     */
    public boolean isTerminated() {
        throw new UnsupportedOperationException("this client does not support orderly shutdown");
    }

    /**
     * Cuts off: the requests in flight fail.
     *
     * @since 21
     */
    public void shutdownNow() {
        throw new UnsupportedOperationException("this client does not support orderly shutdown");
    }

    /**
     * Shuts down in order and waits.
     *
     * <p>It declares no exception, unlike {@link AutoCloseable}'s {@code close}: closing has to fit
     * in a try-with-resources without forcing anything to be caught.
     *
     * @since 21
     */
    public void close() {
        throw new UnsupportedOperationException("this client does not support orderly shutdown");
    }

    /**
     * What to do with a redirect.
     *
     * <p>{@link #NORMAL} is the only one that is not obvious, and it is the one to use: it follows
     * redirects <strong>except</strong> those that go down from HTTPS to HTTP. Following those
     * would turn a secure connection into a cleartext one without anyone asking, which is a known
     * attack.
     */
    public enum Redirect {

        /** Follow none. */
        NEVER,
        /** Follow all, including the one that downgrades to HTTP. */
        ALWAYS,
        /** Follow all except the one that downgrades from HTTPS to HTTP. */
        NORMAL
    }

    /** The protocol version. */
    public enum Version {

        /** HTTP/1.1. */
        HTTP_1_1,
        /**
         * HTTP/2, falling back to 1.1 if the server does not support it.
         *
         * <p>It is a preference and not a requirement: the version is negotiated —by ALPN during
         * the TLS handshake, or by an {@code Upgrade} header over cleartext— so asking for it does
         * not guarantee getting it.
         */
        HTTP_2
    }

    /**
     * Builds an {@link HttpClient}.
     *
     * <p>Everything has a default, so {@code newBuilder().build()} is valid. What gets configured
     * is the exceptions to that.
     */
    public interface Builder {

        /**
         * A selector that uses no proxy at all, not even the system's.
         *
         * <p>It exists because passing nothing does <em>not</em> mean that: unconfigured, the
         * client uses the default selector, which honours the proxy system properties. (This
         * javadoc said environment variables; what the JDK documents is system properties.)
         */
        public static final ProxySelector NO_PROXY = ProxySelector.of(null);

        /** The cookie handler. Without one, the client keeps none. */
        Builder cookieHandler(CookieHandler cookieHandler);

        /** The timeout for connecting. */
        Builder connectTimeout(Duration duration);

        /** The TLS context; without one, the system default. */
        Builder sslContext(SSLContext sslContext);

        /** The TLS parameters. */
        Builder sslParameters(SSLParameters sslParameters);

        /** Where to run the asynchronous tasks; without one, the client builds its own pool. */
        Builder executor(Executor executor);

        /** What to do with redirects; {@link Redirect#NEVER} by default. */
        Builder followRedirects(Redirect policy);

        /** The preferred version. */
        Builder version(Version version);

        /**
         * The priority of HTTP/2 streams, between 1 and 256.
         *
         * @throws IllegalArgumentException if it is out of range
         */
        Builder priority(int priority);

        /** The proxy selector; see {@link #NO_PROXY}. */
        Builder proxy(ProxySelector proxySelector);

        /** The authenticator for {@code 401} and {@code 407} challenges. */
        Builder authenticator(Authenticator authenticator);

        /**
         * Which local address to go out from.
         *
         * <p>It came with a body for compatibility. It is useful on a machine with several
         * interfaces, where which one is used changes the route and sometimes the permission.
         *
         * @since 19
         */
        default Builder localAddress(InetAddress localAddr) {
            throw new UnsupportedOperationException(
                    "this builder does not support setting the local address");
        }

        /** The built client. */
        HttpClient build();
    }
}
