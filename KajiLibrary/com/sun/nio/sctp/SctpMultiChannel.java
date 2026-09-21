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
 * A channel that holds <strong>several</strong> associations at once.
 *
 * <h2>What it does differently</h2>
 *
 * <p>It has no analogue in TCP. An {@link SctpChannel} talks to one end; this one talks to
 * many over the same socket, and the associations go on being created by themselves: sending
 * a message to an address there is not an association with yet establishes it. Hence almost
 * all of its methods carry an extra {@link Association} -- which one is being talked about has
 * to be said.
 *
 * <p>It is the useful form for a server that attends to many peers without a socket for each
 * one.
 *
 * <h2>{@link #branch}, which is the most interesting thing about the class</h2>
 *
 * <p>It takes an association out of this channel and turns it into an {@link SctpChannel} of
 * its own. It serves precisely when one of many connections turns out to be special and it is
 * convenient to treat it separately -- without cutting it off and establishing it again, which
 * is what would have to be done without this method.
 *
 * <h2>What this VM cannot do</h2>
 *
 * <p>{@link #open} throws {@link UnsupportedOperationException}: there is no SCTP stack. See
 * {@link SctpChannel}'s note.
 */
public abstract class SctpMultiChannel extends AbstractSelectableChannel {

    /** For the SCTP implementations. */
    protected SctpMultiChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * A new channel, with no associations.
     *
     * @throws UnsupportedOperationException always, on this VM
     */
    public static SctpMultiChannel open() throws IOException {
        throw new UnsupportedOperationException("this VM does not have an SCTP stack");
    }

    /** The associations that are open now. */
    public abstract Set<Association> associations() throws IOException;

    /** It binds the channel to a local address, with that number of connections waiting. */
    public abstract SctpMultiChannel bind(SocketAddress local, int backlog) throws IOException;

    /** It binds the channel leaving the {@code backlog} at its default. */
    public final SctpMultiChannel bind(SocketAddress local) throws IOException {
        return bind(local, 0);
    }

    /** It adds a local address to all the associations. */
    public abstract SctpMultiChannel bindAddress(InetAddress address) throws IOException;

    /** It takes a local address out of all the associations. */
    public abstract SctpMultiChannel unbindAddress(InetAddress address) throws IOException;

    /** All the local addresses. */
    public abstract Set<SocketAddress> getAllLocalAddresses() throws IOException;

    /** An association's peer addresses. */
    public abstract Set<SocketAddress> getRemoteAddresses(Association association)
            throws IOException;

    /** It starts closing an association; the others go on. */
    public abstract SctpMultiChannel shutdown(Association association) throws IOException;

    /** An option's value, in the scope of an association. */
    public abstract <T> T getOption(SctpSocketOption<T> name, Association association)
            throws IOException;

    /** It fixes an option, in the scope of an association. */
    public abstract <T> SctpMultiChannel setOption(SctpSocketOption<T> name, T value,
            Association association) throws IOException;

    /** The options this channel understands. */
    public abstract Set<SctpSocketOption<?>> supportedOptions();

    /**
     * The operations it admits in a selector.
     *
     * <p>Read and write, not connect: here one does not connect explicitly -- sending to a new
     * address establishes the association by itself.
     */
    public final int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    }

    /** It receives a message from any of the associations. */
    public abstract <T> MessageInfo receive(ByteBuffer dst, T attachment,
            NotificationHandler<T> handler) throws IOException;

    /** It sends a message; if there is no association with that destination, one is established. */
    public abstract int send(ByteBuffer src, MessageInfo messageInfo) throws IOException;

    /** It takes an association out of this channel and returns it as a channel of its own. */
    public abstract SctpChannel branch(Association association) throws IOException;
}
