package java.net;

import java.io.IOException;
import java.io.OutputStream;

// The channel a response is written into the cache through.
//
// It appears on the **writing** side: when a response worth keeping arrives, `ResponseCache.put`
// returns one of these and the client copies the body into its `OutputStream` while handing it to
// whoever asked.
//
// `abort()` is the part that makes the design work: if the connection is cut halfway, what was
// written is rubbish --a truncated response stored as complete is worse than having no cache-- and
// the client says so, so that the cache throws away what was written. A cache without that notice
// would end up serving incomplete responses.
//
// Abstract, with no logic of its own and no network: it is a contract. Nothing omitted.
public abstract class CacheRequest {

    public CacheRequest() {
    }

    /**
     * The stream to write the response's body into.
     *
     * @throws IOException if the cache cannot open it
     */
    public abstract OutputStream getBody() throws IOException;

    /** Discards what has been written so far: the response did not arrive whole and is not to be kept. */
    public abstract void abort();
}
