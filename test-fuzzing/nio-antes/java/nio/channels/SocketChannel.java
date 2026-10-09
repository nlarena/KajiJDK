package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * KajiLibrary's java.nio.channels.SocketChannel — a channel over a TCP connection.
 *
 * <p>What separates it from a `java.net.Socket` is that it can be put into non-blocking mode and
 * put into a {@link Selector}. Out of that come the two oddities that define its life cycle:
 *
 * <ul>
 *   <li>{@link #connect} may come back with `false`. It means "I started, I have not finished yet",
 *       not a failure. Whoever calls it in non-blocking mode has to wait for `OP_CONNECT` and then
 *       call {@link #finishConnect()};
 *   <li>{@link #shutdownOutput()} is not {@link #close()}. It closes **one half**: the other end
 *       sees end of data and can go on sending. It is how a request and response protocol says
 *       goodbye without cutting the answer short.
 * </ul>
 *
 * <h2>The `open()`s, which used not to be there</h2>
 *
 * <p>This file used to say that the VM had no network native at all and that there was therefore no
 * way of making a channel. It has them now, and the three {@code open()}s are there, with the
 * in-house implementation behind them. Also {@link #socket()}, which was missing because it returns
 * `java.net.Socket` and that class did not exist in this tree.
 *
 * <p>The channel that comes out of {@code open()} is the one of the **installed** provider, if
 * somebody installed one through the `spi` mechanism; the in-house one only when there is none. A
 * provider of one's own replaces ours, never the other way round.
 */
public abstract class SocketChannel extends AbstractSelectableChannel
        implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {

    protected SocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * Reading, writing and connecting; never accepting.
     *
     * <p>It is `final` and not abstract because the set does not depend on the state: even if the
     * channel is not connected yet, the operations **valid** for its type are always these three.
     */
    public final int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT;
    }

    /** Ties the channel to a local address. */
    public abstract SocketChannel bind(SocketAddress local) throws IOException;

    /** Sets a socket option. */
    public abstract <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException;

    /**
     * Closes the reading half: whatever arrives afterwards is discarded and `read` returns -1.
     */
    public abstract SocketChannel shutdownInput() throws IOException;

    /** Closes the writing half: the other end sees end of data. */
    public abstract SocketChannel shutdownOutput() throws IOException;

    /** Whether the connection is made. */
    public abstract boolean isConnected();

    /** Whether there is a connection started and unfinished. */
    public abstract boolean isConnectionPending();

    /**
     * Connects to `remote`.
     *
     * @return `true` if it was left connected; `false` if it started and has to be finished with
     *         {@link #finishConnect()}. The second only happens in non-blocking mode
     */
    public abstract boolean connect(SocketAddress remote) throws IOException;

    /**
     * Finishes a started connection.
     *
     * <p>It has to be called even if the selector says it is ready: it is where the error appears
     * if the connection failed. Without this step, a refusal by the other end would look like a
     * connected channel.
     */
    public abstract boolean finishConnect() throws IOException;

    /** The address of the other end, or `null` if it is not connected. */
    public abstract SocketAddress getRemoteAddress() throws IOException;

    public abstract int read(ByteBuffer dst) throws IOException;

    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    public final long read(ByteBuffer[] dsts) throws IOException {
        return this.read(dsts, 0, dsts.length);
    }

    public abstract int write(ByteBuffer src) throws IOException;

    public abstract long write(ByteBuffer[] srcs, int offset, int length) throws IOException;

    public final long write(ByteBuffer[] srcs) throws IOException {
        return this.write(srcs, 0, srcs.length);
    }

    /** The local address, or `null` if it is not tied. */
    public abstract SocketAddress getLocalAddress() throws IOException;

    /**
     * An unconnected channel.
     *
     * <p>The channel is born in blocking mode, as the contract requires: whoever wants the other
     * mode calls {@link #configureBlocking}.
     *
     * @throws IOException if it could not be opened
     */
    public static SocketChannel open() throws IOException {
        return KajiSelectorProvider.current().openSocketChannel();
    }

    /**
     * An unconnected channel of that protocol family.
     *
     * @throws UnsupportedOperationException if the provider does not sustain that family
     * @throws IOException if it could not be opened
     */
    public static SocketChannel open(java.net.ProtocolFamily family) throws IOException {
        return KajiSelectorProvider.current().openSocketChannel(family);
    }

    /**
     * A channel **already connected** to that address.
     *
     * <p>It is the convenience the JDK documents: open, connect in blocking mode, and return. If
     * the connection fails the channel is closed, so as not to leave a descriptor hanging from a
     * call that threw.
     *
     * @throws IOException if it could not be opened or could not be connected
     */
    public static SocketChannel open(SocketAddress remote) throws IOException {
        SocketChannel c = SocketChannel.open();
        try {
            c.connect(remote);
        } catch (RuntimeException e) {
            c.close();
            throw e;
        } catch (IOException e) {
            c.close();
            throw e;
        }
        return c;
    }

    /**
     * The socket that wraps this channel.
     *
     * <p>It shares the descriptor: closing either of the two closes the same socket, which is what
     * the contract promises for the channel/socket pair.
     */
    public abstract java.net.Socket socket();
}
