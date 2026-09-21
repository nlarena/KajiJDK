package jdk.net;

import java.net.SocketOption;

/**
 * Socket options the JDK offers outside the standard set.
 *
 * <h2>What separates them from {@link java.net.StandardSocketOptions}</h2>
 *
 * <p>That <strong>none is guaranteed</strong>. Each one depends on the operating system having it:
 * {@link #TCP_QUICKACK} is of Linux, {@link #SO_PEERCRED} only makes sense on a Unix domain socket,
 * and the three {@code TCP_KEEP*} exist almost everywhere but not everywhere. Asking for one the
 * platform does not support throws {@link UnsupportedOperationException}.
 *
 * <p>That is why they are a set apart and not more constants in the standard class: the standard set
 * is a contract every implementation of Java fulfils, and these are not. Before using them it is
 * advisable to consult the socket's {@code supportedOptions()}.
 */
public final class ExtendedSocketOptions {

    private ExtendedSocketOptions() {
    }

    /**
     * Acknowledge straight away instead of waiting to see whether there is data to send back.
     *
     * <p>TCP delays the ACKs on purpose, so that they can travel stuck to the answer and save a packet.
     * In a request and response protocol that wait is pure latency, and this turns it off. Linux only.
     */
    public static final SocketOption<Boolean> TCP_QUICKACK =
            new Option<Boolean>("TCP_QUICKACK", Boolean.class);

    /** How many seconds of silence before sending the first keep-alive probe. */
    public static final SocketOption<Integer> TCP_KEEPIDLE =
            new Option<Integer>("TCP_KEEPIDLE", Integer.class);

    /** How many seconds between probes. */
    public static final SocketOption<Integer> TCP_KEEPINTERVAL =
            new Option<Integer>("TCP_KEEPINTERVAL", Integer.class);

    /** How many probes with no answer before giving the connection up for dead. */
    public static final SocketOption<Integer> TCP_KEEPCOUNT =
            new Option<Integer>("TCP_KEEPCOUNT", Integer.class);

    /**
     * The NAPI identifier of the interface the packets come in through.
     *
     * <p>It is read-only and serves for one thing: placing the threads that attend a connection near the
     * network queue that receives it. Outside that use it says nothing.
     */
    public static final SocketOption<Integer> SO_INCOMING_NAPI_ID =
            new Option<Integer>("SO_INCOMING_NAPI_ID", Integer.class);

    /**
     * Who is on the other side, on a Unix domain socket.
     *
     * <p>Read-only, and the only option of this class that returns an identity instead of a number. See
     * {@link UnixDomainPrincipal} for why this is possible here and not over TCP.
     */
    public static final SocketOption<UnixDomainPrincipal> SO_PEERCRED =
            new Option<UnixDomainPrincipal>("SO_PEERCRED", UnixDomainPrincipal.class);

    /**
     * Do not fragment: a datagram bigger than the MTU fails instead of being split.
     *
     * <p>It is how the MTU of the path is discovered, and how a protocol that brings its own
     * fragmentation keeps IP from adding another on top.
     */
    public static final SocketOption<Boolean> IP_DONTFRAGMENT =
            new Option<Boolean>("IP_DONTFRAGMENT", Boolean.class);

    /**
     * An option: a name and a type.
     *
     * <p>Private because the set is closed — they are the seven constants above. Making one more would
     * give an object no socket knows how to attend.
     */
    private static class Option<T> implements SocketOption<T> {

        private final String name;
        private final Class<T> type;

        Option(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        public String name() {
            return this.name;
        }

        public Class<T> type() {
            return this.type;
        }

        public String toString() {
            return this.name;
        }
    }
}
