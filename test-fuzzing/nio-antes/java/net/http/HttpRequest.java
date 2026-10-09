package java.net.http;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * An HTTP request already built. Immutable, and therefore reusable between threads.
 *
 * <h2>Why the body is a {@code Publisher} and not a byte array</h2>
 *
 * <p>Because a body may not fit in memory, or may not exist yet when the request is built. A {@link
 * BodyPublisher} is a {@link Flow.Publisher} of {@link ByteBuffer}: the client asks it for the data
 * <em>when it is about to send it</em> and at the pace the network accepts it.
 *
 * <p>That is real backpressure, and it is what allows uploading a file of gigabytes without loading
 * it. The price is that a {@code BodyPublisher} can be subscribed to <strong>more than
 * once</strong> —when following a redirect, when retrying— and that is why {@link
 * BodyPublishers#ofInputStream} takes a {@link Supplier} and not a stream: a stream already
 * consumed cannot be read again, and a supplier can give another.
 *
 * @since 11
 */
public abstract class HttpRequest {

    /** For implementations. */
    protected HttpRequest() {
    }

    /** A request builder with that URI. */
    public static Builder newBuilder(URI uri) {
        return newBuilder().uri(uri);
    }

    /**
     * A builder that starts by copying another request, with its headers filtered.
     *
     * <p>It is for rewriting a request —removing an authorization before following a redirect to
     * another host, for example— without building it all again.
     */
    public static Builder newBuilder(HttpRequest request, BiPredicate<String, String> filter) {
        throw new UnsupportedOperationException(
                "this VM has no HTTP client implementation; see HttpClient");
    }

    /** An empty builder. */
    public static Builder newBuilder() {
        throw new UnsupportedOperationException(
                "this VM has no HTTP client implementation; see HttpClient");
    }

    /** The body, if the request carries one. */
    public abstract Optional<BodyPublisher> bodyPublisher();

    /** The request method. */
    public abstract String method();

    /** The timeout for the whole request, if one was set. */
    public abstract Optional<Duration> timeout();

    /**
     * Whether {@code Expect: 100-continue} was requested.
     *
     * <p>It asks the server whether it will accept the body <em>before</em> sending it. It pays off
     * when the body is large and a refusal likely —an authorization that may fail—, and costs an
     * extra round trip when not.
     */
    public abstract boolean expectContinue();

    /** Where it goes. */
    public abstract URI uri();

    /** The requested version, if one was set. */
    public abstract Optional<HttpClient.Version> version();

    /** The headers. */
    public abstract HttpHeaders headers();

    /**
     * Over the URI, the method, the headers, the timeout, {@code expectContinue} and the version.
     */
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HttpRequest)) {
            return false;
        }
        HttpRequest o = (HttpRequest) obj;
        return uri().equals(o.uri())
                && method().equals(o.method())
                && headers().equals(o.headers())
                && timeout().equals(o.timeout())
                && expectContinue() == o.expectContinue()
                && version().equals(o.version());
    }

    public final int hashCode() {
        return uri().hashCode() * 31 + method().hashCode() * 17 + headers().hashCode();
    }

    /**
     * Builds an {@link HttpRequest}.
     *
     * <p>All methods return {@code this}, so they chain. The difference between {@link #header} and
     * {@link #setHeader} is the usual one and worth not mixing up: the first <em>adds</em> one more
     * value, the second <em>replaces</em> whatever there was.
     */
    public interface Builder {

        /** Where the request goes. */
        Builder uri(URI uri);

        /** It asks for {@code Expect: 100-continue}; see {@link HttpRequest#expectContinue}. */
        Builder expectContinue(boolean enable);

        /** Sets the version to request. */
        Builder version(HttpClient.Version version);

        /** Adds a value to that header, without removing the existing ones. */
        Builder header(String name, String value);

        /**
         * Adds several, as name/value pairs.
         *
         * @throws IllegalArgumentException if the count is odd
         */
        Builder headers(String... headers);

        /** The total timeout; running out of it gives {@link HttpTimeoutException}. */
        Builder timeout(Duration duration);

        /** Leaves that header with that single value. */
        Builder setHeader(String name, String value);

        /** Method {@code GET}, with no body. */
        Builder GET();

        /** Method {@code POST} with that body. */
        Builder POST(BodyPublisher bodyPublisher);

        /** Method {@code PUT} with that body. */
        Builder PUT(BodyPublisher bodyPublisher);

        /** Method {@code DELETE}, with no body. */
        Builder DELETE();

        /**
         * Method {@code HEAD}, with no body.
         *
         * <p>It came with a default body —it is {@code method("HEAD", noBody())}— because adding it
         * as abstract would have broken whoever already implemented this interface.
         */
        default Builder HEAD() {
            return method("HEAD", BodyPublishers.noBody());
        }

        /**
         * Any method.
         *
         * @throws IllegalArgumentException if the method is not a valid HTTP token, or is one the
         *     implementation restricts. This javadoc named {@code CONNECT} and {@code TRACE}; the
         *     JDK leaves the choice to the implementation, and its own restricts only {@code
         *     CONNECT}.
         */
        Builder method(String method, BodyPublisher bodyPublisher);

        /** The built request. */
        HttpRequest build();

        /**
         * An independent copy of this builder.
         *
         * <p>It is for building several requests that share most of the configuration, without
         * changes to one affecting the others.
         */
        Builder copy();
    }

    /**
     * Where the body's bytes come from.
     *
     * <p>A {@link Flow.Publisher} with one thing more: {@link #contentLength}, which the client
     * needs <strong>before</strong> it starts publishing so it can send {@code Content-Length}
     * instead of chunking.
     */
    public interface BodyPublisher extends Flow.Publisher<ByteBuffer> {

        /**
         * How many bytes it will publish.
         *
         * @return the length, or a negative if unknown — and then the body goes in chunks
         */
        long contentLength();
    }

    /**
     * The {@link BodyPublisher}s the JDK provides.
     *
     * <p>In this VM they refuse: there is no HTTP client implementation. See {@link HttpClient}.
     */
    public static class BodyPublishers {

        private BodyPublishers() {
        }

        private static BodyPublisher refuse() {
            throw new UnsupportedOperationException(
                    "this VM has no HTTP client implementation; see HttpClient");
        }

        /** Wraps a custom publisher, with unknown length. */
        public static BodyPublisher fromPublisher(
                Flow.Publisher<? extends ByteBuffer> publisher) {
            return refuse();
        }

        /** The same, declaring the length. */
        public static BodyPublisher fromPublisher(
                Flow.Publisher<? extends ByteBuffer> publisher, long contentLength) {
            return refuse();
        }

        /** The body is that string, in UTF-8. */
        public static BodyPublisher ofString(String body) {
            return refuse();
        }

        /** The same, with that charset. */
        public static BodyPublisher ofString(String s, Charset charset) {
            return refuse();
        }

        /**
         * The body comes from a stream the supplier gives.
         *
         * <p>A {@link Supplier} and not a stream directly: see the {@link HttpRequest} note on why
         * a body can be requested more than once.
         */
        public static BodyPublisher ofInputStream(Supplier<? extends InputStream> streamSupplier) {
            return refuse();
        }

        /** The body is those bytes. */
        public static BodyPublisher ofByteArray(byte[] buf) {
            return refuse();
        }

        /** A slice of those bytes. */
        public static BodyPublisher ofByteArray(byte[] buf, int offset, int length) {
            return refuse();
        }

        /** The body is the content of that file. */
        public static BodyPublisher ofFile(Path path) throws java.io.FileNotFoundException {
            return refuse();
        }

        /** The body is those blocks, one after another. */
        public static BodyPublisher ofByteArrays(Iterable<byte[]> iter) {
            return refuse();
        }

        /**
         * No body.
         *
         * <p>This javadoc said it is what {@code GET}, {@code DELETE} and {@code HEAD} use. The
         * JDK's own builder gives those three no publisher at all; only the interface's default
         * {@code HEAD} uses this.
         */
        public static BodyPublisher noBody() {
            return refuse();
        }

        /**
         * Several bodies, one after another.
         *
         * <p>The total length is known only if <strong>all</strong> know theirs; with a single
         * unknown one, the result is unknown too.
         */
        public static BodyPublisher concat(BodyPublisher... publishers) {
            return refuse();
        }
    }
}
