package java.net;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// The point where the whole VM's cookie handling is plugged in.
//
// It is abstract and has a static registry, just like `Authenticator`, and for the same reason:
// whoever makes HTTP requests cannot receive the cookie policy as a parameter --it is ten layers
// further down-- so it looks for it in an agreed place.
//
// The API speaks in **raw headers** (`Map<String, List<String>>`) and not in `HttpCookie` objects,
// which looks like a step backwards until the reason shows: that way the handler can deal with
// cookies the platform cannot model, and the HTTP client needs to know nothing about cookies -- it
// passes the headers it received and pastes in the ones it gets back.
//
// Registering and consulting a callback is pure computation, and `CookieManager` implements the real
// work without touching the network. What KajiJDK lacks is an HTTP client to call this, and that is
// not part of this contract. Nothing omitted.
public abstract class CookieHandler {

    private static CookieHandler cookieHandler;

    public CookieHandler() {
    }

    /** The installed handler, or null if there is none. */
    public static synchronized CookieHandler getDefault() {
        return cookieHandler;
    }

    /** Installs the whole VM's handler. */
    public static synchronized void setDefault(CookieHandler cHandler) {
        cookieHandler = cHandler;
    }

    /**
     * The cookie headers to be sent in a request to {@code uri}.
     *
     * @param requestHeaders the headers the client has already assembled, read-only
     * @return a map from header name to values; typically with the key "Cookie"
     */
    public abstract Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders)
            throws IOException;

    /**
     * Stores the cookies a response from {@code uri} brought.
     *
     * @param responseHeaders the response's headers; "Set-Cookie" and "Set-Cookie2" are the ones that
     *     matter
     */
    public abstract void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException;
}
