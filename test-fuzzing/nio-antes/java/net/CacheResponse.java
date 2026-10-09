package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

// A response that came out of the cache instead of the network.
//
// It is the **reading** side: `ResponseCache.get` returns one of these when it has stored what is
// being asked for, and the client uses it instead of connecting.
//
// That it returns the headers and not only the body is not a detail: without them there is no way to
// know whether what was stored is still current (`Expires`, `Cache-Control`, `ETag`), nor to
// revalidate it against the server, nor to hand over the right `Content-Type`. A cache that stored
// only bytes would serve content without knowing what it is or how long it holds.
//
// Abstract, with no logic of its own and no network: it is a contract. Nothing omitted.
//
// Its subclass `SecureCacheResponse` used to be missing, and the reason was not the class itself but
// what its seven members name: five of them return or throw `javax.net.ssl` types --`SSLSession`,
// `SSLPeerUnverifiedException`-- and that package did not exist in this tree. A method declared with
// a type that does not exist does not compile, and creating those two classes just to be able to name
// them would have been inventing the facade of a layer that was not behind it. The package exists now
// and so does the subclass.
public abstract class CacheResponse {

    public CacheResponse() {
    }

    /**
     * The stored response's headers.
     *
     * <p>The null key, if present, is the status line ("HTTP/1.1 200 OK"), which has no header name.
     * It is the same convention as {@link URLConnection#getHeaderFields}.
     *
     * @throws IOException if they cannot be read from the cache
     */
    public abstract Map<String, List<String>> getHeaders() throws IOException;

    /**
     * The stored body.
     *
     * @throws IOException if it cannot be opened
     */
    public abstract InputStream getBody() throws IOException;
}
