package com.sun.nio.sctp;

/**
 * An SCTP association: the relation between two ends, with its streams.
 *
 * <h2>What SCTP contributes over TCP, and why this object is needed</h2>
 *
 * <p>A TCP connection is a single stream of bytes, and that brings head-of-line blocking: a
 * lost segment holds up everything that came behind it, even though it was independent. An
 * SCTP association carries <strong>several streams</strong> in parallel, each one with an order
 * of its own, so a loss in one does not stop the others.
 *
 * <p>That is the reason this object exists and that a descriptor is not enough: an association
 * has an identity ({@link #associationID}) and a negotiated capacity of how many streams it
 * admits in each direction, and those numbers are needed in order to know which
 * {@code streamNumber} is valid in a {@link MessageInfo}.
 *
 * <p>The maxima are <strong>asymmetric</strong> on purpose: each end declares how many streams
 * it accepts to receive, and the two declarations do not have to agree.
 */
public class Association {

    private final int associationID;
    private final int maxInStreams;
    private final int maxOutStreams;

    /**
     * For the SCTP implementations.
     *
     * <p>{@code protected} because an association is created by the protocol's stack when it is
     * negotiated, not by user code: making one by hand would give an object that describes no
     * real connection.
     */
    protected Association(int associationID, int maxInStreams, int maxOutStreams) {
        this.associationID = associationID;
        this.maxInStreams = maxInStreams;
        this.maxOutStreams = maxOutStreams;
    }

    /** The identifier the local stack gave it. Unique while the association lives. */
    public final int associationID() {
        return this.associationID;
    }

    /** How many incoming streams it admits. */
    public final int maxInboundStreams() {
        return this.maxInStreams;
    }

    /** How many outgoing streams it admits. */
    public final int maxOutboundStreams() {
        return this.maxOutStreams;
    }
}
