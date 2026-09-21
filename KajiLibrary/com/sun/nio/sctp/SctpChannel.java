package com.sun.nio.sctp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * A channel over a single SCTP association.
 *
 * <h2>The equivalent of a {@code SocketChannel}, with two differences</h2>
 *
 * <p>The first is <strong>multihoming</strong>: {@link #bindAddress} and
 * {@link #unbindAddress} add and take out local addresses <em>while the association is
 * open</em>. A {@code SocketChannel} is bound to an address and stays there; this one may go
 * on changing the set, which is what gives it fault tolerance without reconnecting.
 *
 * <p>The second is that sending and receiving go by <strong>messages</strong>, not by bytes:
 * hence {@link #send} and {@link #receive} carry a {@link MessageInfo} and are not a byte
 * channel's {@code read}/{@code write}.
 *
 * <h2>Why {@link #receive} receives a notification handler</h2>
 *
 * <p>Because while a message is being waited for, events of the association may arrive, and
 * there is no other moment at which the program looks at the channel. The
 * {@link NotificationHandler} attends to them right there and decides, with its
 * {@link HandlerResult}, whether the wait goes on.
 *
 * <h2>What this VM cannot do</h2>
 *
 * <p>The three {@link #open}s throw {@link UnsupportedOperationException}: SCTP is a protocol
 * of the operating system and this VM has no stack for it. It is not something that is fixed
 * by writing more Java. The class is left with the exact shape of the JDK's -- the abstract
 * methods are declarations and promise nothing -- and the only thing that declines is making a
 * channel that afterwards would talk to nobody.
 */
public abstract class SctpChannel extends AbstractSelectableChannel {

    /** For the SCTP implementations. */
    protected SctpChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * An unconnected channel.
     *
     * @throws UnsupportedOperationException always, on this VM -- see the class note
     */
    public static SctpChannel open() throws IOException {
        throw new UnsupportedOperationException("this VM does not have an SCTP stack");
    }

    /**
     * A channel connected to {@code remote}, asking for that number of streams.
     *
     * @throws UnsupportedOperationException always, on this VM
     */
    public static SctpChannel open(SocketAddress remote, int maxOutStreams, int maxInStreams)
            throws IOException {
        throw new UnsupportedOperationException("this VM does not have an SCTP stack");
    }

    /** The association, or {@code null} if it is not connected yet. */
    public abstract Association association() throws IOException;

    /** It binds the channel to a local address; {@code null} lets the system choose. */
    public abstract SctpChannel bind(SocketAddress local) throws IOException;

    /** It adds a local address to the association. See the multihoming in the class note. */
    public abstract SctpChannel bindAddress(InetAddress address) throws IOException;

    /** It takes a local address out of the association. */
    public abstract SctpChannel unbindAddress(InetAddress address) throws IOException;

    /** It connects to {@code remote}; {@code false} if the connection is left pending. */
    public abstract boolean connect(SocketAddress remote) throws IOException;

    /** The same, asking for that number of streams. */
    public abstract boolean connect(SocketAddress remote, int maxOutStreams, int maxInStreams)
            throws IOException;

    /** Whether there is a connection started and not finished. */
    public abstract boolean isConnectionPending();

    /** It finishes a pending connection. */
    public abstract boolean finishConnect() throws IOException;

    /** All the association's local addresses. */
    public abstract Set<SocketAddress> getAllLocalAddresses() throws IOException;

    /** All the peer's addresses. */
    public abstract Set<SocketAddress> getRemoteAddresses() throws IOException;

    /** It starts closing the association in an orderly way. */
    public abstract SctpChannel shutdown() throws IOException;

    /** An option's value. */
    public abstract <T> T getOption(SctpSocketOption<T> name) throws IOException;

    /** It fixes an option. */
    public abstract <T> SctpChannel setOption(SctpSocketOption<T> name, T value) throws IOException;

    /** The options this channel understands. */
    public abstract Set<SctpSocketOption<?>> supportedOptions();

    /**
     * The operations this channel admits in a selector.
     *
     * <p>The same three as a {@code SocketChannel}: read, write and connect. It is {@code final}
     * because it does not depend on the implementation but on the kind of channel.
     */
    public final int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT;
    }

    /**
     * It receives a message, attending on the way to the notifications that arrive.
     *
     * @param handler who attends to the notifications; {@code null} in order to ignore them
     * @return the message's descriptor, or {@code null} if the handler said
     *     {@link HandlerResult#RETURN}
     */
    public abstract <T> MessageInfo receive(ByteBuffer dst, T attachment,
            NotificationHandler<T> handler) throws IOException;

    /** It sends the contents of {@code src} as a message. */
    public abstract int send(ByteBuffer src, MessageInfo messageInfo) throws IOException;
}
