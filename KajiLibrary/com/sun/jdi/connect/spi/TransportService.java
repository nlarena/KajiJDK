package com.sun.jdi.connect.spi;

import java.io.IOException;

/**
 * A transport a debugger and a VM may talk JDWP through.
 *
 * <h2>The two roles, and why they are asymmetrical</h2>
 *
 * <p>A debugging connection is established in two ways and this class has methods for both:
 * {@link #attach} goes looking for somebody that is already waiting, and the pair
 * {@link #startListening}/{@link #accept} starts waiting. The asymmetry is not of style:
 * whoever listens needs an address <em>before</em> anybody connects, and that is why
 * {@code startListening} returns a {@link ListenKey} instead of blocking. The address comes
 * from there, it is passed to the other side by any means, and only then is {@code accept}
 * called.
 *
 * <h2>Why {@link #capabilities} instead of throwing exceptions</h2>
 *
 * <p>Not every transport supports everything. Shared memory, for instance, cannot always impose
 * a handshake timeout. A transport might accept the parameter and throw an exception on using
 * it; instead of that, {@link Capabilities} allows it to be asked <em>beforehand</em>. The
 * difference is practical: the caller may choose another transport instead of finding out
 * halfway that the timeout it asked for is not respected.
 *
 * <p>When a capability is missing, the corresponding parameter is not silently ignored -- that
 * would be the worst of both worlds --: it is rejected with {@link IllegalArgumentException} if
 * it is not zero.
 */
public abstract class TransportService {

    /** For the transport implementations. */
    public TransportService() {
    }

    /** The transport's name, as whoever chooses it names it. */
    public abstract String name();

    /** A description to show a person. */
    public abstract String description();

    /** What this transport can do; see the note in the class's description. */
    public abstract Capabilities capabilities();

    /**
     * It connects to a VM that is already waiting at {@code address}.
     *
     * @param attachTimeout milliseconds to establish the connection, or {@code 0} to wait with no
     *     limit
     * @param handshakeTimeout milliseconds to complete the JDWP handshake once connected. It is a
     *     separate timeout because they are two different failures: not arriving and arriving at
     *     something that does not speak the protocol
     * @throws IllegalArgumentException if a timeout is negative, or if it is positive and the
     *     transport does not support that capability
     */
    public abstract Connection attach(String address, long attachTimeout, long handshakeTimeout)
            throws IOException;

    /**
     * It starts listening at {@code address}.
     *
     * <p>It does not block: it returns at once with the key {@link #accept} needs, and from which
     * the real address that has to be given to the other side comes.
     */
    public abstract ListenKey startListening(String address) throws IOException;

    /** It starts listening at an address the transport chooses. */
    public abstract ListenKey startListening() throws IOException;

    /**
     * It stops listening.
     *
     * <p>The connections already accepted stay alive: this closes the door, not what has already
     * come in.
     */
    public abstract void stopListening(ListenKey listenKey) throws IOException;

    /**
     * It waits for somebody to connect to what {@code listenKey} is listening at.
     *
     * @throws IllegalStateException if that key has already been given {@link #stopListening}
     */
    public abstract Connection accept(ListenKey listenKey, long acceptTimeout,
            long handshakeTimeout) throws IOException;

    /**
     * What a transport can do.
     *
     * <p>Abstract and not an interface, nor a record of four {@code boolean}s, because that way
     * the JDK may add capabilities to it without breaking whoever has already extended it -- the
     * price of adding a method to an abstract class is paid only by whoever wants to answer it.
     */
    public abstract static class Capabilities {

        public Capabilities() {
        }

        /** Whether one and the same {@link ListenKey} may accept more than one connection. */
        public abstract boolean supportsMultipleConnections();

        /** Whether {@link TransportService#attach} respects its {@code attachTimeout}. */
        public abstract boolean supportsAttachTimeout();

        /** Whether {@link TransportService#accept} respects its {@code acceptTimeout}. */
        public abstract boolean supportsAcceptTimeout();

        /** Whether both respect their {@code handshakeTimeout}. */
        public abstract boolean supportsHandshakeTimeout();
    }

    /**
     * What {@link TransportService#startListening} returns and
     * {@link TransportService#accept} consumes.
     *
     * <p>It is an object and not the address in text for a reason: the address that was left does
     * not have to be the one that was asked for. Listening at port {@code 0} makes the system
     * choose one, and {@link #address} is how which one is found out.
     */
    public abstract static class ListenKey {

        public ListenKey() {
        }

        /** The address it is really listening at. */
        public abstract String address();
    }
}
