package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * KajiLibrary's java.nio.channels.ServerSocketChannel — a channel that listens for connections.
 *
 * <p>It neither reads nor writes: the only thing it does is {@link #accept()}, and that is why it
 * does not implement {@link ByteChannel}. A listening channel with `read` would be a signature that
 * never makes sense.
 *
 * <p>{@link #accept()} in non-blocking mode returns `null` when there is nobody waiting. It is the
 * only way of saying it without an exception, and it is what makes the selector loop possible: wake
 * up, accept, and if `null` came, carry on.
 *
 * <h2>State in this library</h2>
 *
 * <p>The two static {@code open()}s and {@code socket()} **are here**, and this note used to say
 * they were not, because this VM had no network natives and `java.net.ServerSocket` did not exist
 * in this tree. Both exist now, so the ones here are the same as in {@link SocketChannel}: the
 * installed provider's channel, and the in-house one when there is none.
 *
 * <p>The rest of the class is the contract any listening implementation has to fulfil, and it fits
 * with the rest of `java.nio.channels` without changing anything.
 */
public abstract class ServerSocketChannel extends AbstractSelectableChannel
        implements NetworkChannel {

    protected ServerSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /** Accepting only: a listening channel is never "ready to read". */
    public final int validOps() {
        return SelectionKey.OP_ACCEPT;
    }

    /** Ties the channel to `local` with the queue of pending ones the system prefers. */
    public final ServerSocketChannel bind(SocketAddress local) throws IOException {
        return this.bind(local, 0);
    }

    /**
     * Ties the channel to `local`.
     *
     * @param backlog how many connections can wait without being accepted; `0` or less lets the
     *        system choose. It is not a limit on connections but on the ones that pile up
     *        **unattended**
     */
    public abstract ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException;

    /** Sets a socket option. */
    public abstract <T> ServerSocketChannel setOption(SocketOption<T> name, T value)
            throws IOException;

    /**
     * Accepts a connection.
     *
     * @return the channel of the accepted connection, or `null` in non-blocking mode if there was
     *     none
     */
    public abstract SocketChannel accept() throws IOException;

    /** The address it is tied to, or `null` if it is not tied. */
    public abstract SocketAddress getLocalAddress() throws IOException;

    /**
     * An untied listening channel.
     *
     * <p>It is born in blocking mode, as the contract requires.
     *
     * @throws IOException if it could not be opened
     */
    public static ServerSocketChannel open() throws IOException {
        return KajiSelectorProvider.current().openServerSocketChannel();
    }

    /**
     * An untied listening channel of that protocol family.
     *
     * @throws UnsupportedOperationException if the provider does not sustain that family
     * @throws IOException if it could not be opened
     */
    public static ServerSocketChannel open(java.net.ProtocolFamily family) throws IOException {
        return KajiSelectorProvider.current().openServerSocketChannel(family);
    }

    /**
     * The socket that wraps this channel.
     *
     * <p>It shares the descriptor: closing either of the two closes the same socket.
     */
    public abstract java.net.ServerSocket socket();
}
