package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * KajiLibrary's java.nio.channels.AsynchronousSocketChannel — an asynchronous TCP connection.
 *
 * <p>Against {@link SocketChannel} what changes is who waits: there the program asks the selector
 * whether there is anything; here the operation gives notice when it has finished. There is no
 * blocking mode to configure and no {@link Selector} to register in, and that is why this class is
 * **not** a {@link SelectableChannel}.
 *
 * <p>The rule that costs the most: <strong>one read and one write pending per channel</strong>.
 * Asking for a second read without the first having finished is {@link ReadPendingException}. It is
 * severe on purpose: with two reads in flight over one same stream of bytes, the order in which they
 * complete decides where the bytes fall, and nobody controls that.
 *
 * <p>The time limits go per operation and not per channel, which is what allows what one really
 * wants: waiting two seconds for the header and thirty for the body. On running out, the operation
 * fails with {@link InterruptedByTimeoutException} and --importantly-- <strong>the channel is left
 * useless</strong>: there is no knowing how many bytes it managed to move, so going on using it
 * would be going on over a misaligned stream.
 *
 * <h2>State in this library</h2>
 *
 * <p><strong>Both {@code open()}s are here.</strong> This note used to say the VM had no network
 * natives; it has them, and {@link SocketChannel} really connects. Underneath there is that blocking
 * channel and a thread pool --see {@code AsyncSocketChannelImpl}--, which is how the JDK implements
 * this class on the platforms without asynchronous input and output of the system.
 */
public abstract class AsynchronousSocketChannel implements AsynchronousByteChannel, NetworkChannel {

    private final AsynchronousChannelProvider provider;

    protected AsynchronousSocketChannel(AsynchronousChannelProvider provider) {
        this.provider = provider;
    }

    /**
     * One of the default group.
     *
     * @return the channel
     * @throws IOException if it cannot be opened
     */
    public static AsynchronousSocketChannel open() throws IOException {
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
    public static AsynchronousSocketChannel open(AsynchronousChannelGroup group) throws IOException {
        final AsynchronousChannelProvider p = group == null
                ? AsynchronousChannelProvider.provider() : group.provider();
        return p.openAsynchronousSocketChannel(group);
    }

    /** The provider that made it. */
    public final AsynchronousChannelProvider provider() {
        return this.provider;
    }

    /** Ties the channel to a local address. */
    public abstract AsynchronousSocketChannel bind(SocketAddress local) throws IOException;

    /** Sets a socket option. */
    public abstract <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value)
            throws IOException;

    /** Closes the reading half. */
    public abstract AsynchronousSocketChannel shutdownInput() throws IOException;

    /** Closes the writing half; the other end sees end of data. */
    public abstract AsynchronousSocketChannel shutdownOutput() throws IOException;

    /** The address of the other end, or `null` if it is not connected. */
    public abstract SocketAddress getRemoteAddress() throws IOException;

    /** Connects to `remote` and tells `handler`. */
    public abstract <A> void connect(SocketAddress remote, A attachment,
            CompletionHandler<Void, ? super A> handler);

    /** Like the other one, returning a {@link Future}. */
    public abstract Future<Void> connect(SocketAddress remote);

    /**
     * Reads with a time limit.
     *
     * @param timeout `0` or less means no limit. On running out, the channel is left useless; see the
     *        note of the class
     */
    public abstract <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment,
            CompletionHandler<Integer, ? super A> handler);

    /** Like the other one, with no time limit. */
    public final <A> void read(ByteBuffer dst, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        this.read(dst, 0L, TimeUnit.MILLISECONDS, attachment, handler);
    }

    /** Reads returning a {@link Future}, with no time limit. */
    public abstract Future<Integer> read(ByteBuffer dst);

    /**
     * Reads spreading into several buffers.
     *
     * <p>It returns `Long` and not `Integer` because the total can go past two gigabytes: they are
     * several buffers, not one.
     */
    public abstract <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout,
            TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler);

    /** Writes with a time limit. The same caveats as the read. */
    public abstract <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment,
            CompletionHandler<Integer, ? super A> handler);

    /** Like the other one, with no time limit. */
    public final <A> void write(ByteBuffer src, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        this.write(src, 0L, TimeUnit.MILLISECONDS, attachment, handler);
    }

    /** Writes returning a {@link Future}, with no time limit. */
    public abstract Future<Integer> write(ByteBuffer src);

    /** Writes gathering several buffers. */
    public abstract <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout,
            TimeUnit unit, A attachment, CompletionHandler<Long, ? super A> handler);

    /** The local address, or `null` if it is not tied. */
    public abstract SocketAddress getLocalAddress() throws IOException;
}
