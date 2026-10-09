package javax.management.remote;

import java.io.IOException;

/**
 * KajiLibrary's javax.management.remote.JMXServerErrorException -- the server threw an
 * {@link Error}.
 *
 * <p>It exists because of a concrete problem of remote calls. A server-side {@code Error} --out of
 * memory, a missing class-- cannot be propagated as such to the client: over there it would mean
 * that <b>the client</b> is broken, and it is not.
 *
 * <p>So it is wrapped in an {@link IOException}, which is what the client is already prepared to
 * catch when it talks over the network. The cause is still the original {@code Error}, so that it
 * can be seen.
 *
 * <p>The only constructor demands the {@code Error}: without it the class would make no sense.
 */
public class JMXServerErrorException extends IOException {

    private static final long serialVersionUID = 3996732239558744666L;

    /** The server's. */
    private Error cause = null;

    /**
     * @param s the message
     * @param err the server's error
     */
    public JMXServerErrorException(String s, Error err) {
        super(s);
        this.cause = err;
    }

    /** The server's error. */
    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
