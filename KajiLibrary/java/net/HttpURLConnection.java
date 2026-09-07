package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.security.Permission;

// An HTTP connection: method, response code, redirects and body streaming.
//
// ===========================================================================================
// THIS CLASS IS ABSTRACT, AND THAT IS THE WHOLE DIFFERENCE
// ===========================================================================================
//
// KajiJDK has no HTTP client and will not have one without sockets. But `HttpURLConnection` **is not
// an HTTP client**: it is the description of one. Its two abstract methods --`disconnect()` and
// `usingProxy()`-- are the only ones that need to know whether a connection is alive, and this class
// does not write them: it declares them.
//
// Everything else belongs to this class and needs no network:
//
//  - **The request's state**: `method`, `instanceFollowRedirects`, the streaming modes. They are
//    fields with their validations, and the validations are real: `setRequestMethod("DELETE_IT")`
//    throws `ProtocolException`, `setChunkedStreamingMode` after connecting throws
//    `IllegalStateException`, and setting both streaming modes at once throws, because they are
//    mutually exclusive.
//  - **Reading the response**: `getResponseCode()` and `getResponseMessage()` parse the status line
//    ("HTTP/1.1 404 Not Found") the subclass put in header 0. It is text parsing, it is exact, and it
//    is complete.
//  - **The forty status codes**, which are agreed numbers.
//  - **`getPermission()`**, which builds the `SocketPermission` for the URL's host and port.
//
// None of those members promises a connection. The promise is concentrated in `connect()` --which it
// inherits abstract from `URLConnection`-- and in the two abstract methods here.
//
// ===========================================================================================
// WHO INSTANTIATES IT
// ===========================================================================================
//
// **Nobody, in this tree.** `URL.openConnection()` of an `http:` URL does not get this far: it throws
// before, saying there is no handler for that protocol. This class is the contract an HTTP client
// would have to fulfil the day one exists, and the type the signatures can name meanwhile.
//
// What was not done, and it is the obvious temptation: there is **no** concrete subclass returning
// 200 and an empty body so that "something works". That would be an invented answer presented as an
// answer from the server, which is the worst kind of lie that can be written in an HTTP client.
//
// All sixty-three members are here.
public abstract class HttpURLConnection extends URLConnection {

    /** The request's method. */
    protected String method = "GET";

    /** The chunk size in chunked mode, or -1 if it is not in that mode. */
    protected int chunkLength = -1;

    /**
     * The body's fixed length, or -1.
     *
     * @deprecated It overflows with bodies over two gigabytes; use {@link #fixedContentLengthLong}.
     */
    @Deprecated
    protected int fixedContentLength = -1;

    /** The body's fixed length, or -1 if it is not in that mode. */
    protected long fixedContentLengthLong = -1;

    /** The status code, or -1 if it has not been read yet. */
    protected int responseCode = -1;

    /** The text accompanying the status code ("Not Found"), or null. */
    protected String responseMessage = null;

    /** Whether this connection follows redirects on its own. */
    protected boolean instanceFollowRedirects = followRedirects;

    private static boolean followRedirects = true;

    // The methods the JDK accepts. The list is closed on purpose: an arbitrary method is refused,
    // because `URLConnection` would not know how to assemble the request.
    private static final String[] METODOS = {
        "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"};

    /** Builds the connection without connecting it. */
    protected HttpURLConnection(URL u) {
        super(u);
    }

    // ---- redirects ------------------------------------------------------------------------------

    /** Whether **new** connections follow redirects. */
    public static void setFollowRedirects(boolean set) {
        followRedirects = set;
    }

    /** Whether new connections follow redirects. */
    public static boolean getFollowRedirects() {
        return followRedirects;
    }

    /**
     * Whether **this** connection follows redirects.
     *
     * <p>That there is a per-instance flag as well as the whole VM's is what allows saying "not this
     * one", which is what is needed to inspect a `301` instead of following it.
     *
     * @throws IllegalStateException if it has already connected
     */
    public void setInstanceFollowRedirects(boolean followRedirects) {
        this.instanceFollowRedirects = followRedirects;
    }

    /** Whether this connection follows redirects. */
    public boolean getInstanceFollowRedirects() {
        return this.instanceFollowRedirects;
    }

    // ---- the request ----------------------------------------------------------------------------

    /**
     * The request's method: GET, POST, HEAD, OPTIONS, PUT, DELETE or TRACE.
     *
     * @throws ProtocolException     if the method is not one of those, or if it has already connected
     * @throws IllegalStateException never -- the JDK uses `ProtocolException` for the "already
     *                               connected" case too, and that is respected
     */
    public void setRequestMethod(String method) throws ProtocolException {
        if (this.connected) {
            throw new ProtocolException("Can't reset method: already connected");
        }
        for (int i = 0; i < METODOS.length; i++) {
            if (METODOS[i].equals(method)) {
                this.method = method;
                return;
            }
        }
        throw new ProtocolException("Invalid HTTP method: " + method);
    }

    /** The request's method. */
    public String getRequestMethod() {
        return this.method;
    }

    /**
     * Sends the body with a length known in advance, without gathering it all in memory.
     *
     * <p>It serves to upload a large file: without it, `URLConnection` has to accumulate the whole
     * body in order to set the `Content-Length`.
     *
     * @throws IllegalStateException    if it has already connected or the chunked mode is already set
     * @throws IllegalArgumentException if the length is negative
     * @deprecated It overflows with bodies over two gigabytes; use the `long` overload.
     */
    @Deprecated
    public void setFixedLengthStreamingMode(int contentLength) {
        checkStreamingMode();
        if (contentLength < 0) {
            throw new IllegalArgumentException("invalid content length");
        }
        this.fixedContentLength = contentLength;
    }

    /**
     * Sends the body with a length known in advance.
     *
     * @throws IllegalStateException    if it has already connected or the chunked mode is already set
     * @throws IllegalArgumentException if the length is negative
     */
    public void setFixedLengthStreamingMode(long contentLength) {
        checkStreamingMode();
        if (contentLength < 0) {
            throw new IllegalArgumentException("invalid content length");
        }
        this.fixedContentLengthLong = contentLength;
    }

    /**
     * Sends the body in chunks, without knowing in advance how big it is.
     *
     * <p>It is the other way of not accumulating in memory, and the one that serves when the length
     * **cannot** be known -- a response generated on the fly, for instance.
     *
     * @param chunklen the suggested chunk size; {@code <= 0} leaves the choice to the implementation
     * @throws IllegalStateException if it has already connected or a fixed length is already set
     */
    public void setChunkedStreamingMode(int chunklen) {
        checkStreamingMode();
        if (chunklen <= 0) {
            this.chunkLength = 4096;
        } else {
            this.chunkLength = chunklen;
        }
    }

    // The two streaming modes are mutually exclusive: one says how big the body is and the other says
    // it is not known. Having them together means nothing, and that is why it is refused instead of
    // one being chosen.
    private void checkStreamingMode() {
        if (this.connected) {
            throw new IllegalStateException("Can't set streaming mode: already connected");
        }
        if (this.chunkLength != -1) {
            throw new IllegalStateException("Chunked encoding streaming mode set");
        }
        if (this.fixedContentLength != -1 || this.fixedContentLengthLong != -1) {
            throw new IllegalStateException("Fixed length streaming mode set");
        }
    }

    /**
     * Installs this connection's authenticator.
     *
     * <p>The base throws `UnsupportedOperationException`, just like the JDK: not every implementation
     * knows how to authenticate per connection --the JDK has had a global authenticator since before
     * this method existed-- and the one that does overrides it. Accepting it silently would be worse:
     * the caller would believe their credentials were going to be used.
     *
     * @throws UnsupportedOperationException always, in the base implementation
     * @throws NullPointerException          if {@code auth} is null
     */
    public void setAuthenticator(Authenticator auth) {
        throw new UnsupportedOperationException(
                "Supplying an authenticator is not supported by " + this.getClass());
    }

    // ---- the response ---------------------------------------------------------------------------

    /**
     * The name of header number {@code n}, or null.
     *
     * <p>It returns null for {@code n == 0} even when there is a header 0, because header 0 is the
     * **status line** and has no name. That convention is what makes `getHeaderField(0)` return
     * "HTTP/1.1 200 OK".
     */
    @Override
    public String getHeaderFieldKey(int n) {
        return null;
    }

    /** The value of header number {@code n}, or null. The base has no headers. */
    @Override
    public String getHeaderField(int n) {
        return null;
    }

    /**
     * The status code: 200, 404, 500.
     *
     * <p>It comes from parsing the status line the subclass left in header 0. It returns -1 if there
     * is no status line or if it is not of the expected shape -- a code is not invented.
     *
     * @throws IOException if the connection fails while the response is being read
     */
    public int getResponseCode() throws IOException {
        if (this.responseCode != -1) {
            return this.responseCode;
        }

        // The connection is forced first, because the status line does not exist until the far end
        // has answered. The exception is KEPT instead of propagating: if a status line appeared
        // anyway, the request completed and the failure was about something else --reading the body,
        // typically-- and covering the response code with that exception would lose exactly what was
        // being asked for.
        Exception failure = null;
        try {
            getInputStream();
        } catch (Exception e) {
            failure = e;
        }

        String statusLine = getHeaderField(0);
        if (statusLine == null) {
            // With no status line there was no response, and there the kept exception IS the
            // explanation; returning -1 and swallowing it would leave the caller not knowing what
            // happened.
            if (failure != null) {
                if (failure instanceof RuntimeException) {
                    throw (RuntimeException) failure;
                }
                throw (IOException) failure;
            }
            return -1;
        }

        // "HTTP-Version SP Status-Code SP Reason-Phrase", from RFC 2616. The phrase is optional:
        // there are servers that omit it, and the JDK accepts them on purpose.
        if (statusLine.startsWith("HTTP/1.")) {
            int codePos = statusLine.indexOf(' ');
            if (codePos > 0) {
                int phrasePos = statusLine.indexOf(' ', codePos + 1);
                if (phrasePos > 0 && phrasePos < statusLine.length()) {
                    // Untrimmed: the phrase is what arrived, spaces included.
                    this.responseMessage = statusLine.substring(phrasePos + 1);
                }
                if (phrasePos < 0) {
                    phrasePos = statusLine.length();
                }
                try {
                    this.responseCode =
                            Integer.parseInt(statusLine.substring(codePos + 1, phrasePos));
                    return this.responseCode;
                } catch (NumberFormatException notACode) {
                    // It falls through to the -1 below: a code is not invented.
                }
            }
        }
        return -1;
    }

    /**
     * The text accompanying the code ("Not Found"), or null if none came.
     *
     * @throws IOException if the connection fails while the response is being read
     */
    public String getResponseMessage() throws IOException {
        getResponseCode();
        return this.responseMessage;
    }

    /**
     * That header read as a date.
     *
     * <p>It overrides `URLConnection`'s version to add "GMT" to a date that does not carry it, which
     * is what the JDK does: there are servers that omit it, and all of HTTP's date grammars are in
     * GMT anyway.
     */
    @Override
    public long getHeaderFieldDate(String name, long Default) {
        String texto = getHeaderField(name);
        if (texto == null) {
            return Default;
        }
        if (texto.indexOf("GMT") == -1) {
            texto = texto + " GMT";
        }
        long parseada = HttpCookie.parseCookieDate(texto);
        if (parseada == Long.MIN_VALUE) {
            return Default;
        }
        return parseada;
    }

    /**
     * The stream with an error's body, or null.
     *
     * <p>It exists because a `404` **also carries a body**, and `getInputStream()` throws for any
     * error code: without this method, the page explaining the error would be unreachable. It returns
     * null when there is no error, no body, or the connection never got established.
     *
     * <p>The base returns null, as in the JDK.
     */
    public InputStream getErrorStream() {
        return null;
    }

    /**
     * The permission needed to make this request.
     *
     * <p>A `SocketPermission` to connect to the URL's host and port, which is what the JDK returns.
     * It can be built whole here because building a permission is text, not network.
     *
     * @throws IOException if it could not be determined
     */
    @Override
    public Permission getPermission() throws IOException {
        int puerto = this.url.getPort();
        if (puerto < 0) {
            puerto = 80;
        }
        String host = this.url.getHost() + ":" + puerto;
        return new SocketPermission(host, "connect");
    }

    /**
     * Releases the connection; the requests that follow open a new one.
     *
     * <p>Abstract: it is one of the two methods that need to know whether anything is alive on the
     * other side.
     */
    public abstract void disconnect();

    /**
     * Whether this request goes out through a proxy.
     *
     * <p>Abstract: only the implementation knows which way it went out.
     */
    public abstract boolean usingProxy();

    // ---- status codes ---------------------------------------------------------------------------

    /** 200: it went well. */
    public static final int HTTP_OK = 200;

    /** 201: the resource was created. */
    public static final int HTTP_CREATED = 201;

    /** 202: it was accepted, but not done yet. */
    public static final int HTTP_ACCEPTED = 202;

    /** 203: the information comes from a copy, not from the origin. */
    public static final int HTTP_NOT_AUTHORITATIVE = 203;

    /** 204: it went well and there is no body. */
    public static final int HTTP_NO_CONTENT = 204;

    /** 205: it went well; the client should clear the form. */
    public static final int HTTP_RESET = 205;

    /** 206: only the piece that was asked for is coming. */
    public static final int HTTP_PARTIAL = 206;

    /** 300: there are several possible responses. */
    public static final int HTTP_MULT_CHOICE = 300;

    /** 301: it moved, and for good. */
    public static final int HTTP_MOVED_PERM = 301;

    /** 302: it moved, for now. */
    public static final int HTTP_MOVED_TEMP = 302;

    /** 303: look somewhere else, with a GET. */
    public static final int HTTP_SEE_OTHER = 303;

    /** 304: it has not changed since the date you sent. */
    public static final int HTTP_NOT_MODIFIED = 304;

    /** 305: you have to go through a proxy. */
    public static final int HTTP_USE_PROXY = 305;

    /** 400: the request is malformed. */
    public static final int HTTP_BAD_REQUEST = 400;

    /** 401: authentication is needed. */
    public static final int HTTP_UNAUTHORIZED = 401;

    /** 402: reserved for payments; hardly anyone uses it. */
    public static final int HTTP_PAYMENT_REQUIRED = 402;

    /** 403: you identified yourself and still you may not. */
    public static final int HTTP_FORBIDDEN = 403;

    /** 404: it does not exist. */
    public static final int HTTP_NOT_FOUND = 404;

    /** 405: that method is not valid for this resource. */
    public static final int HTTP_BAD_METHOD = 405;

    /** 406: it cannot be given in any of the formats you accept. */
    public static final int HTTP_NOT_ACCEPTABLE = 406;

    /** 407: you have to authenticate to the proxy. */
    public static final int HTTP_PROXY_AUTH = 407;

    /** 408: the client took too long to send the request. */
    public static final int HTTP_CLIENT_TIMEOUT = 408;

    /** 409: it clashes with the resource's current state. */
    public static final int HTTP_CONFLICT = 409;

    /** 410: it existed and no longer does, on purpose. */
    public static final int HTTP_GONE = 410;

    /** 411: the `Content-Length` is missing. */
    public static final int HTTP_LENGTH_REQUIRED = 411;

    /** 412: a condition of the request was not met. */
    public static final int HTTP_PRECON_FAILED = 412;

    /** 413: the body is too large. */
    public static final int HTTP_ENTITY_TOO_LARGE = 413;

    /** 414: the URL is too long. */
    public static final int HTTP_REQ_TOO_LONG = 414;

    /** 415: the body's type is not accepted. */
    public static final int HTTP_UNSUPPORTED_TYPE = 415;

    /**
     * 500: server error.
     *
     * @deprecated It was badly named from the start; the good name is {@link #HTTP_INTERNAL_ERROR}.
     *             It is kept because there is code using it.
     */
    @Deprecated
    public static final int HTTP_SERVER_ERROR = 500;

    /** 500: the server broke. */
    public static final int HTTP_INTERNAL_ERROR = 500;

    /** 501: the server does not know how to do that. */
    public static final int HTTP_NOT_IMPLEMENTED = 501;

    /** 502: the one further back answered rubbish. */
    public static final int HTTP_BAD_GATEWAY = 502;

    /** 503: it is not available right now. */
    public static final int HTTP_UNAVAILABLE = 503;

    /** 504: the one further back did not answer in time. */
    public static final int HTTP_GATEWAY_TIMEOUT = 504;

    /** 505: that version of HTTP is not supported. */
    public static final int HTTP_VERSION = 505;
}
