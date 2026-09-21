package com.sun.nio.sctp;

import java.net.SocketAddress;

/**
 * Everything that accompanies an SCTP message and is not its bytes.
 *
 * <h2>Why a message needs this and a TCP byte does not</h2>
 *
 * <p>TCP delivers a stream: the only associated datum is how many bytes there are. SCTP
 * delivers <strong>messages</strong>, and each one carries which stream it goes by, whether it
 * goes ordered, how long it is worth going on trying, and with which application protocol it is
 * going to be interpreted on the other side. None of that fits in the {@code ByteBuffer}.
 *
 * <h2>The setters return {@code this}</h2>
 *
 * <p>{@link #streamNumber(int)} and its siblings return the same object, not a new one, and
 * that allows the configuration to be written on one line:
 *
 * <pre>{@code
 * MessageInfo.createOutgoing(destination, 3).unordered(true).timeToLive(500)
 * }</pre>
 *
 * <p>It is worth knowing that it <strong>mutates</strong>: it is not a value object. Reusing a
 * {@code MessageInfo} between two sends shares the changes.
 */
public abstract class MessageInfo {

    /** For the SCTP implementations. */
    protected MessageInfo() {
    }

    /**
     * A message to send to {@code address} over the stream {@code streamNumber}.
     *
     * <p>The address may be {@code null} when the channel already knows where it is going -- a
     * connected {@link SctpChannel} -- and it is needed in a {@link SctpMultiChannel}, which talks
     * to several ends.
     *
     * @throws IllegalArgumentException if {@code streamNumber} is negative or goes over
     *     {@code 65536}
     */
    public static MessageInfo createOutgoing(SocketAddress address, int streamNumber) {
        return new Outgoing(null, address, streamNumber);
    }

    /**
     * The same, but over a concrete association of a {@link SctpMultiChannel}.
     *
     * @throws IllegalArgumentException if {@code streamNumber} is out of range
     */
    public static MessageInfo createOutgoing(Association association, SocketAddress address,
            int streamNumber) {
        if (association == null) {
            throw new IllegalArgumentException("the association is needed");
        }
        return new Outgoing(association, address, streamNumber);
    }

    /** Where it came from or where it is going. */
    public abstract SocketAddress address();

    /** The association, or {@code null} if there is not one yet. */
    public abstract Association association();

    /** How many bytes the message has. */
    public abstract int bytes();

    /**
     * Whether the message is whole.
     *
     * <p>A {@code receive} may return an incomplete message when the buffer it was given was not
     * enough. Ignoring this is the easiest way of processing half a message as if it were one.
     */
    public abstract boolean isComplete();

    /** It marks whether it is whole. */
    public abstract MessageInfo complete(boolean complete);

    /** Whether it goes with no order with respect to the others of its stream. */
    public abstract boolean isUnordered();

    /**
     * It marks whether it goes with no order.
     *
     * <p>It is the knob that changes the treatment: a message with no order is delivered as soon
     * as it arrives, without waiting for those that went before it in its stream.
     */
    public abstract MessageInfo unordered(boolean unordered);

    /**
     * The application protocol's identifier.
     *
     * <p>SCTP does not look at it: it carries it and delivers it. It serves so that the two ends
     * should agree on how to interpret the bytes without spending a header of their own.
     */
    public abstract int payloadProtocolID();

    /** It fixes the application protocol identifier. */
    public abstract MessageInfo payloadProtocolID(int ppid);

    /** Which stream it goes or came by. */
    public abstract int streamNumber();

    /**
     * It fixes the stream.
     *
     * @throws IllegalArgumentException if it is out of range
     */
    public abstract MessageInfo streamNumber(int streamNumber);

    /** How many milliseconds it is worth going on trying; {@code 0} is with no limit. */
    public abstract long timeToLive();

    /**
     * It fixes the time to live.
     *
     * <p>Once it has expired, the message is discarded and a {@link SendFailedNotification}
     * arrives. It is what makes SCTP useful for data that age -- telemetry, audio -- where
     * retrying for ever is worse than losing.
     */
    public abstract MessageInfo timeToLive(long millis);

    /**
     * The implementation of an outgoing message.
     *
     * <p>Private because nobody should be able to create one outside the two factories: the JDK
     * does the same. The <em>incoming</em> messages are built by the stack, not by this class.
     */
    private static final class Outgoing extends MessageInfo {

        private final Association association;
        private final SocketAddress address;
        private int streamNumber;
        private boolean complete = true;
        private boolean unordered;
        private int ppid;
        private long timeToLive;

        Outgoing(Association association, SocketAddress address, int streamNumber) {
            checkStream(streamNumber);
            this.association = association;
            this.address = address;
            this.streamNumber = streamNumber;
        }

        private static void checkStream(int streamNumber) {
            if (streamNumber < 0 || streamNumber > 65536) {
                throw new IllegalArgumentException("stream out of range: "
                        + String.valueOf(streamNumber));
            }
        }

        public SocketAddress address() {
            return this.address;
        }

        public Association association() {
            return this.association;
        }

        // An outgoing message has no bytes yet: they are held by the `ByteBuffer` that is passed to
                    // the `send`. The JDK answers zero for the same reason.
        public int bytes() {
            return 0;
        }

        public boolean isComplete() {
            return this.complete;
        }

        public MessageInfo complete(boolean complete) {
            this.complete = complete;
            return this;
        }

        public boolean isUnordered() {
            return this.unordered;
        }

        public MessageInfo unordered(boolean unordered) {
            this.unordered = unordered;
            return this;
        }

        public int payloadProtocolID() {
            return this.ppid;
        }

        public MessageInfo payloadProtocolID(int ppid) {
            this.ppid = ppid;
            return this;
        }

        public int streamNumber() {
            return this.streamNumber;
        }

        public MessageInfo streamNumber(int streamNumber) {
            checkStream(streamNumber);
            this.streamNumber = streamNumber;
            return this;
        }

        public long timeToLive() {
            return this.timeToLive;
        }

        public MessageInfo timeToLive(long millis) {
            this.timeToLive = millis;
            return this;
        }
    }
}
