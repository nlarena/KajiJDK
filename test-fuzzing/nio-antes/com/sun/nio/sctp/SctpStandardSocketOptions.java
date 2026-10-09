package com.sun.nio.sctp;

import java.net.SocketAddress;

/**
 * The socket options the JDK defines for SCTP.
 *
 * <p>Each constant is an object with a name and a type, not a string nor an integer, and that
 * is what makes {@code setOption(SCTP_NODELAY, 5)} not compile: the value's type travels in the
 * option's type. It is the same design as {@link java.net.StandardSocketOptions}.
 */
public class SctpStandardSocketOptions {

    private SctpStandardSocketOptions() {
    }

    /**
     * How many streams to ask for in each direction when negotiating the association.
     *
     * <p>It has to be fixed <strong>before</strong> connecting: an association's maxima are
     * negotiated when establishing it and afterwards do not move. It is the reason it is an object
     * of two numbers and not two loose options -- the two go in the same negotiation message.
     */
    public static class InitMaxStreams {

        private final int maxInStreams;
        private final int maxOutStreams;

        private InitMaxStreams(int maxInStreams, int maxOutStreams) {
            this.maxInStreams = maxInStreams;
            this.maxOutStreams = maxOutStreams;
        }

        /**
         * @throws IllegalArgumentException if either is negative or goes over {@code 65535}
         */
        public static InitMaxStreams create(int maxInStreams, int maxOutStreams) {
            if (maxOutStreams < 0 || maxOutStreams > 65535) {
                throw new IllegalArgumentException("maxOutStreams out of range: "
                        + String.valueOf(maxOutStreams));
            }
            if (maxInStreams < 0 || maxInStreams > 65535) {
                throw new IllegalArgumentException("maxInStreams out of range: "
                        + String.valueOf(maxInStreams));
            }
            return new InitMaxStreams(maxInStreams, maxOutStreams);
        }

        /** How many incoming streams to ask for. */
        public int maxInStreams() {
            return this.maxInStreams;
        }

        /** How many outgoing streams to ask for. */
        public int maxOutStreams() {
            return this.maxOutStreams;
        }

        public String toString() {
            return "[maxInStreams:" + String.valueOf(this.maxInStreams)
                    + ", maxOutStreams:" + String.valueOf(this.maxOutStreams) + "]";
        }

        public boolean equals(Object obj) {
            if (obj instanceof InitMaxStreams) {
                InitMaxStreams other = (InitMaxStreams) obj;
                return other.maxInStreams == this.maxInStreams
                        && other.maxOutStreams == this.maxOutStreams;
            }
            return false;
        }

        public int hashCode() {
            // The two fields fit in 16 bits each, so concatenating them is injective: two
                        // different pairs cannot collide. A `31 * a + b` could.
            return (this.maxInStreams << 16) | this.maxOutStreams;
        }
    }

    /** Not to fragment: a message bigger than the MTU fails instead of being split. */
    public static final SctpSocketOption<Boolean> SCTP_DISABLE_FRAGMENTS =
            new Option<Boolean>("SCTP_DISABLE_FRAGMENTS", Boolean.class);

    /** A message is sent only when whoever sends it marks it complete. */
    public static final SctpSocketOption<Boolean> SCTP_EXPLICIT_COMPLETE =
            new Option<Boolean>("SCTP_EXPLICIT_COMPLETE", Boolean.class);

    /** How much the messages of different streams are interleaved when delivering them. */
    public static final SctpSocketOption<Integer> SCTP_FRAGMENT_INTERLEAVE =
            new Option<Integer>("SCTP_FRAGMENT_INTERLEAVE", Integer.class);

    /** How many streams to ask for when negotiating; see {@link InitMaxStreams}. */
    public static final SctpSocketOption<InitMaxStreams> SCTP_INIT_MAXSTREAMS =
            new Option<InitMaxStreams>("SCTP_INIT_MAXSTREAMS", InitMaxStreams.class);

    /** To send at once instead of gathering small messages. SCTP's {@code TCP_NODELAY}. */
    public static final SctpSocketOption<Boolean> SCTP_NODELAY =
            new Option<Boolean>("SCTP_NODELAY", Boolean.class);

    /** Which of the peer's addresses to use by default. */
    public static final SctpSocketOption<SocketAddress> SCTP_PRIMARY_ADDR =
            new Option<SocketAddress>("SCTP_PRIMARY_ADDR", SocketAddress.class);

    /** To ask the peer to use this address of ours as the primary one. */
    public static final SctpSocketOption<SocketAddress> SCTP_SET_PEER_PRIMARY_ADDR =
            new Option<SocketAddress>("SCTP_SET_PEER_PRIMARY_ADDR", SocketAddress.class);

    /** Size of the sending buffer. */
    public static final SctpSocketOption<Integer> SO_SNDBUF =
            new Option<Integer>("SO_SNDBUF", Integer.class);

    /** Size of the receiving buffer. */
    public static final SctpSocketOption<Integer> SO_RCVBUF =
            new Option<Integer>("SO_RCVBUF", Integer.class);

    /** How long to wait on closing for what was left pending to go out. */
    public static final SctpSocketOption<Integer> SO_LINGER =
            new Option<Integer>("SO_LINGER", Integer.class);

    /**
     * An option: a name and a type.
     *
     * <p>Private because the set of options is closed -- they are the ten constants above -- and
     * letting more be made would give objects no implementation knows how to attend to.
     */
    private static class Option<T> implements SctpSocketOption<T> {

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
