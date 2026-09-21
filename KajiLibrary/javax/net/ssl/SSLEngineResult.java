package javax.net.ssl;

/**
 * What a {@code wrap} or an {@code unwrap} of {@link SSLEngine} returned.
 *
 * <h2>Why two statuses are needed and not one</h2>
 *
 * <p>Because an {@link SSLEngine} answers two questions at once, and they are independent. {@link
 * Status} says <strong>what happened with this call</strong> --whether it consumed, whether it
 * lacked room, whether the engine is closed--. {@link HandshakeStatus} says <strong>what has to be
 * done next</strong>, which is what governs the loop of whoever uses it.
 *
 * <p>Mixing them would be the classic error: a call may end {@code OK} and still need another
 * {@code wrap} before anything useful happens, because the handshake is still in progress. They are
 * two axes.
 */
public class SSLEngineResult {

    /**
     * How the call ended.
     *
     * <p>The first two are not errors but <strong>requests</strong>: the engine cannot go on with
     * the buffers it was given and they have to be enlarged or emptied and the call retried.
     * Treating them as failures is the most common way of writing an {@code SSLEngine} loop
     * wrongly.
     */
    public enum Status {

        /**
         * Input is missing: an incomplete record arrived. More has to be read from the network and
         * retried.
         */
        BUFFER_UNDERFLOW,
        /** Room is missing in the output. The destination buffer has to be emptied and retried. */
        BUFFER_OVERFLOW,
        /** It went fine. */
        OK,
        /** The engine is closed in that direction. */
        CLOSED
    }

    /**
     * What has to be done next.
     *
     * <p>It is the status that drives the loop. {@link #NEED_TASK} is the easiest to overlook: it
     * means the engine has heavy work pending --asymmetric cryptography-- that it deliberately does
     * <em>not</em> do in the calling thread, so as not to block it. It has to be taken with {@link
     * SSLEngine#getDelegatedTask} and run, otherwise the handshake never advances.
     */
    public enum HandshakeStatus {

        /** There is no handshake in progress. */
        NOT_HANDSHAKING,
        /** The handshake just finished. It is reported only once. */
        FINISHED,
        /**
         * There are pending tasks; take them with {@link SSLEngine#getDelegatedTask} and run them.
         */
        NEED_TASK,
        /** The engine needs to produce data: call {@code wrap}. */
        NEED_WRAP,
        /** The engine needs to consume data: call {@code unwrap}. */
        NEED_UNWRAP,
        /**
         * Like {@link #NEED_UNWRAP}, but without reading anything new from the network.
         *
         * <p>It exists because of DTLS, which runs over datagrams: the engine may hold inside a
         * message that arrived out of order and that it can now process. Reading from the network
         * here would block waiting for something already at hand.
         */
        NEED_UNWRAP_AGAIN
    }

    private final Status status;
    private final HandshakeStatus handshakeStatus;
    private final int bytesConsumed;
    private final int bytesProduced;
    private final long sequenceNumber;

    /**
     * @throws IllegalArgumentException if a status is {@code null} or a count is negative
     */
    public SSLEngineResult(Status status, HandshakeStatus handshakeStatus, int bytesConsumed,
            int bytesProduced) {
        this(status, handshakeStatus, bytesConsumed, bytesProduced, -1L);
    }

    /**
     * The same, with the record's sequence number — only meaningful in DTLS.
     *
     * @throws IllegalArgumentException if a status is {@code null} or a count is negative
     */
    public SSLEngineResult(Status status, HandshakeStatus handshakeStatus, int bytesConsumed,
            int bytesProduced, long sequenceNumber) {
        if (status == null) {
            throw new IllegalArgumentException("the status is missing");
        }
        if (handshakeStatus == null) {
            throw new IllegalArgumentException("the handshake status is missing");
        }
        if (bytesConsumed < 0 || bytesProduced < 0) {
            throw new IllegalArgumentException("the byte counts cannot be negative");
        }
        this.status = status;
        this.handshakeStatus = handshakeStatus;
        this.bytesConsumed = bytesConsumed;
        this.bytesProduced = bytesProduced;
        this.sequenceNumber = sequenceNumber;
    }

    /** How the call ended. */
    public final Status getStatus() {
        return this.status;
    }

    /** What has to be done next. */
    public final HandshakeStatus getHandshakeStatus() {
        return this.handshakeStatus;
    }

    /** How many bytes were read from the input. */
    public final int bytesConsumed() {
        return this.bytesConsumed;
    }

    /** How many bytes were written to the output. */
    public final int bytesProduced() {
        return this.bytesProduced;
    }

    /**
     * The record's sequence number, unsigned.
     *
     * <p>{@code -1} when it does not apply: in TLS over TCP the transport already guarantees order
     * and there is nothing to number. It is a {@code long} read as <strong>unsigned</strong>, so
     * comparing it with {@code <} goes wrong for high values — {@link Long#compareUnsigned} has to
     * be used.
     */
    public final long sequenceNumber() {
        return this.sequenceNumber;
    }

    public String toString() {
        return "Status = " + this.status.toString()
                + " HandshakeStatus = " + this.handshakeStatus.toString()
                + "\nbytesConsumed = " + String.valueOf(this.bytesConsumed)
                + " bytesProduced = " + String.valueOf(this.bytesProduced)
                + (this.sequenceNumber == -1L ? ""
                        : " sequenceNumber = " + Long.toUnsignedString(this.sequenceNumber));
    }
}
