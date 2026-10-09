package java.net;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// The whole VM's response cache.
//
// The same pattern as `CookieHandler` and `Authenticator`: abstract with a static registry, because
// whoever makes the request is too far down to receive the cache as a parameter.
//
// The two methods are the cycle's two halves. `get` is called **before** connecting: if it returns a
// `CacheResponse`, there is no connection. `put` is called **after** receiving, and returns the
// channel to write into --or null, which means "do not store this one". Deciding what is stored is
// the cache's business, not the client's, and that is why `put` may refuse.
//
// The registry and the lookup are pure computation and are complete. What KajiJDK lacks is an HTTP
// client to call this; that is not part of this contract. Nothing omitted.
public abstract class ResponseCache {

    private static ResponseCache theResponseCache;

    public ResponseCache() {
    }

    /** The installed cache, or null if there is none. */
    public static synchronized ResponseCache getDefault() {
        return theResponseCache;
    }

    /** Installs the whole VM's cache; null uninstalls it. */
    public static synchronized void setDefault(ResponseCache responseCache) {
        theResponseCache = responseCache;
    }

    /**
     * The stored response for that request, or null if there is no usable one.
     *
     * @param uri            the resource being asked for
     * @param rqstMethod     the request's method ("GET")
     * @param rqstHeaders    the request's headers, which may change which response applies
     * @throws IOException if reading from the cache fails
     */
    public abstract CacheResponse get(URI uri, String rqstMethod, Map<String, List<String>> rqstHeaders)
            throws IOException;

    /**
     * Offers the cache the chance to store {@code conn}'s response.
     *
     * @return where to write the body, or null if the cache decides not to store it
     * @throws IOException if writing fails
     */
    public abstract CacheRequest put(URI uri, URLConnection conn) throws IOException;
}
