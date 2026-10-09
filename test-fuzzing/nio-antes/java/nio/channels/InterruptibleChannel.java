package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.InterruptibleChannel — a channel that can be interrupted.
 *
 * <p>It marks the channel as asynchronously closeable and interruptible, and the two things go
 * together for a concrete reason: if a thread is blocked reading, the only way of getting it out of
 * there is closing the channel underneath it. That is why interrupting a thread blocked in one of
 * these **closes the channel** and throws {@link ClosedByInterruptException} at it.
 *
 * <p>It may look abrupt, and it is deliberate: an I/O operation abandoned halfway leaves the
 * channel in a state nobody can describe --how many bytes were read, where the other end was left--
 * so it is closed instead of being handed over like that.
 *
 * <p>{@link #close()} is redeclared to document that any thread blocked in this channel wakes up
 * with {@link AsynchronousCloseException}.
 *
 */
public interface InterruptibleChannel extends Channel {

    /** Closes the channel; the threads blocked in it wake up with an exception. */
    void close() throws java.io.IOException;
}
