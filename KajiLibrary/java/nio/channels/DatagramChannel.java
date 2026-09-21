package java.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * KajiLibrary's java.nio.channels.DatagramChannel — a channel of datagrams (UDP).
 *
 * <p>It has two sets of operations and mixing them is the typical mistake:
 *
 * <ul>
 *   <li>{@link #send} and {@link #receive} carry the address in each call. It is the normal use of
 *       UDP: each packet goes wherever it says;
 *   <li>{@link #read} and {@link #write} do not carry it, and that is why they **demand** that
 *       {@link #connect} has been called first. A `read` without connecting is
 *       {@link NotYetConnectedException}.
 * </ul>
 *
 * <p>{@link #connect} over a datagram channel negotiates nothing with anybody --UDP has no
 * connection-- but rather **filters**: it fixes the other end, and from there the system discards
 * whatever comes from any other. It serves for that and because, once fixed, sending comes out
 * cheaper: the address does not have to be resolved for each packet.
 *
 * <p>A detail of {@link #receive} that surprises: if the datagram does not fit in the buffer, **the
 * rest is lost in silence**. There is no partial read to continue, because a datagram is all or
 * nothing.
 *
 * <h2>State in this library</h2>
 *
 * <p>The two static {@code open()}s and {@code socket()} **are here**, and this note used to say
 * they were not, for want of network natives in this VM; the natives are there and so are they.
 *
 * <p>The class implements {@link MulticastChannel}, and its two `join`s are here too: they were
 * missing because they take a `java.net.NetworkInterface` that did not exist in this tree, and it
 * does exist now. See the note of that interface.
 */
public abstract class DatagramChannel extends AbstractSelectableChannel
        implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, MulticastChannel {

    protected DatagramChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * Reading and writing; a datagram channel never connects or accepts in the selector's sense.
     */
    public final int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    }

    /** Ties the channel to a local address. `null` lets the system choose. */
    public abstract DatagramChannel bind(SocketAddress local) throws IOException;

    /** Sets a socket option. */
    public abstract <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException;

    /** Whether it is tied to a remote end. */
    public abstract boolean isConnected();

    /**
     * Fixes the other end: it filters what arrives and makes what goes out cheaper. See the note
     * of the class.
     */
    public abstract DatagramChannel connect(SocketAddress remote) throws IOException;

    /** Undoes {@link #connect}; the channel goes back to accepting from anybody. */
    public abstract DatagramChannel disconnect() throws IOException;

    /** The fixed end, or `null` if there is none. */
    public abstract SocketAddress getRemoteAddress() throws IOException;

    /**
     * Receives a datagram.
     *
     * @return where it came from, or `null` in non-blocking mode if there was none
     */
    public abstract SocketAddress receive(ByteBuffer dst) throws IOException;

    /**
     * Sends whatever is left in `src` as a datagram to `target`.
     *
     * @return the bytes sent, or `0` in non-blocking mode if there was no room to send
     */
    public abstract int send(ByteBuffer src, SocketAddress target) throws IOException;

    /** Reads from the fixed end. It demands a previous {@link #connect}. */
    public abstract int read(ByteBuffer dst) throws IOException;

    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    public final long read(ByteBuffer[] dsts) throws IOException {
        return this.read(dsts, 0, dsts.length);
    }

    /** Writes to the fixed end. It demands a previous {@link #connect}. */
    public abstract int write(ByteBuffer src) throws IOException;

    public abstract long write(ByteBuffer[] srcs, int offset, int length) throws IOException;

    public final long write(ByteBuffer[] srcs) throws IOException {
        return this.write(srcs, 0, srcs.length);
    }

    /** The address it is tied to, or `null`. */
    public abstract SocketAddress getLocalAddress() throws IOException;

    /**
     * An untied datagram channel.
     *
     * <p>It is born in blocking mode, as the contract requires.
     *
     * @throws IOException if it could not be opened
     */
    public static DatagramChannel open() throws IOException {
        return KajiSelectorProvider.current().openDatagramChannel();
    }

    /**
     * An untied datagram channel of that protocol family.
     *
     * @throws UnsupportedOperationException if the provider does not sustain that family
     * @throws IOException if it could not be opened
     */
    public static DatagramChannel open(java.net.ProtocolFamily family) throws IOException {
        return KajiSelectorProvider.current().openDatagramChannel(family);
    }

    /**
     * The socket that wraps this channel.
     *
     * <p>It shares the descriptor: closing either of the two closes the same socket.
     */
    public abstract java.net.DatagramSocket socket();
}
