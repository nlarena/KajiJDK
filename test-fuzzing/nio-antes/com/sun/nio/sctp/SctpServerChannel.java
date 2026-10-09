package com.sun.nio.sctp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 * The channel that listens and accepts associations, one by one.
 *
 * <p>It is the analogue of a {@code ServerSocketChannel}, and the alternative to
 * {@link SctpMultiChannel}: there the associations live together in a single channel, here
 * each {@link #accept} returns an {@link SctpChannel} of its own. Which is convenient depends
 * on whether the server wants to treat each peer separately or attend to them all from the
 * same place.
 *
 * <p>It has neither {@code send} nor {@code receive}: no messages go over a channel that only
 * listens.
 *
 * <p>{@link #open} throws {@link UnsupportedOperationException} on this VM; see
 * {@link SctpChannel}'s note.
 */
public abstract class SctpServerChannel extends AbstractSelectableChannel {

    /** For the SCTP implementations. */
    protected SctpServerChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * A new listening channel, unbound.
     *
     * @throws UnsupportedOperationException always, on this VM
     */
    public static SctpServerChannel open() throws IOException {
        throw new UnsupportedOperationException("this VM does not have an SCTP stack");
    }

    /** It waits for an association and returns it as a channel of its own. */
    public abstract SctpChannel accept() throws IOException;

    /** It binds the channel leaving the {@code backlog} at its default. */
    public final SctpServerChannel bind(SocketAddress local) throws IOException {
        return bind(local, 0);
    }

    /** It binds the channel, with that number of associations waiting. */
    public abstract SctpServerChannel bind(SocketAddress local, int backlog) throws IOException;

    /** It adds a local address. */
    public abstract SctpServerChannel bindAddress(InetAddress address) throws IOException;

    /** It takes a local address out. */
    public abstract SctpServerChannel unbindAddress(InetAddress address) throws IOException;

    /** All the local addresses. */
    public abstract Set<SocketAddress> getAllLocalAddresses() throws IOException;

    /** An option's value. */
    public abstract <T> T getOption(SctpSocketOption<T> name) throws IOException;

    /** It fixes an option. */
    public abstract <T> SctpServerChannel setOption(SctpSocketOption<T> name, T value)
            throws IOException;

    /** The options this channel understands. */
    public abstract Set<SctpSocketOption<?>> supportedOptions();

    /** The only thing a listening channel admits in a selector: accepting. */
    public final int validOps() {
        return SelectionKey.OP_ACCEPT;
    }
}
