package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.AsynchronousChannel — a channel whose operations do not wait.
 *
 * <p>The difference from an ordinary non-blocking channel is not one of degree but of shape: a
 * non-blocking one **does what it can now** and returns how much it did, while an asynchronous one
 * **accepts the whole request** and tells afterwards, through a `Future` or a
 * {@link CompletionHandler}.
 *
 * <p>{@link #close()} is redeclared to document what happens with what was left pending: every
 * operation under way finishes with {@link AsynchronousCloseException}. That guarantee is what
 * makes closing enough to clean up --no request is left waiting for an answer that is not going to
 * come--.
 *
 */
public interface AsynchronousChannel extends Channel {

    /** Closes the channel; what is pending finishes with an exception, not in silence. */
    void close() throws java.io.IOException;
}
