package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.Future;

/**
 * KajiLibrary's java.nio.channels.AsynchronousServerSocketChannel — an asynchronous listening
 * channel.
 *
 * <p>It only accepts; it neither reads nor writes. {@link #accept()} asks for **one** connection, it
 * does not open a stream of connections: to go on accepting it has to be called again, and the
 * normal thing is to do it from the same {@link CompletionHandler} that attended the previous one.
 * That re-asking inside the handler is the idiomatic way of using this class, and forgetting it
 * produces a server that attends exactly one connection.
 *
 * <p>As in {@link AsynchronousSocketChannel}, there is **a single pending operation** at a time: a
 * second `accept` before the first has finished is {@link AcceptPendingException}.
 *
 * <h2>State in this library</h2>
 *
 * <p>Both {@code open()}s **are here**, and this note used to say they were not, for want of network
 * natives in this VM. They are there, so both are as well: they go through the asynchronous provider
 * and underneath there is the blocking listening channel with a thread pool.
 */
public abstract class AsynchronousServerSocketChannel
        implements AsynchronousChannel, NetworkChannel {

    private final AsynchronousChannelProvider provider;

    protected AsynchronousServerSocketChannel(AsynchronousChannelProvider provider) {
        this.provider = provider;
    }

    /**
     * One of the default group.
     *
     * @return the channel
     * @throws IOException if it cannot be opened
     */
    public static AsynchronousServerSocketChannel open() throws IOException {
        return open(null);
    }

    /**
     * One of that group.
     *
     * @param group the group, or {@code null} for the default one
     * @return the channel
     * @throws IOException if it cannot be opened
     * @throws ShutdownChannelGroupException if the group no longer accepts channels
     */
    public static AsynchronousServerSocketChannel open(AsynchronousChannelGroup group) throws IOException {
        final AsynchronousChannelProvider p = group == null
                ? AsynchronousChannelProvider.provider() : group.provider();
        return p.openAsynchronousServerSocketChannel(group);
    }

    /** The provider that made it. */
    public final AsynchronousChannelProvider provider() {
        return this.provider;
    }

    /** Ties the channel to `local` with the queue of pending ones the system prefers. */
    public final AsynchronousServerSocketChannel bind(SocketAddress local) throws IOException {
        return this.bind(local, 0);
    }

    /**
     * Ties the channel to `local`.
     *
     * @param backlog how many connections can wait without being accepted; `0` or less lets the system
     *        choose
     */
    public abstract AsynchronousServerSocketChannel bind(SocketAddress local, int backlog)
            throws IOException;

    /** Sets a socket option. */
    public abstract <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value)
            throws IOException;

    /** Accepts **one** connection and tells `handler`. See the note of the class. */
    public abstract <A> void accept(A attachment,
            CompletionHandler<AsynchronousSocketChannel, ? super A> handler);

    /** Like the other one, returning a {@link Future}. */
    public abstract Future<AsynchronousSocketChannel> accept();

    /** The address it is tied to, or `null` if it is not tied. */
    public abstract SocketAddress getLocalAddress() throws IOException;
}
