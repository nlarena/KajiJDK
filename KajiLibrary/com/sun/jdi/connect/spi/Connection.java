package com.sun.jdi.connect.spi;

import java.io.IOException;

/**
 * A channel of JDWP packets already established between the debugger and the debugged VM.
 *
 * <h2>What it is and what it is not</h2>
 *
 * <p>It is deliberately narrow: four methods, and none of them knows what a packet says. A
 * {@code Connection} carries arrays of bytes and nothing more -- who talks first, what each
 * field means and how request and answer correspond is the business of the JDWP protocol, which
 * lives one layer above. That separation is what allows the same debugger to work over a TCP
 * socket, over shared memory or over any transport somebody writes.
 *
 * <h2>{@link #readPacket}'s contract</h2>
 *
 * <p>It returns <strong>one</strong> complete packet, not whatever has arrived. Reassembling
 * what the transport fragmented is the responsibility of whoever implements it, and it is the
 * part that makes this interface worth while: without it every user would have to know that TCP
 * does not respect message boundaries.
 *
 * <p>An array of length zero means <em>end of stream</em>: the other side closed in an orderly
 * way. It is different from {@link ClosedConnectionException}, which means that this connection
 * closed on this side or broke.
 */
public abstract class Connection {

    /** For the transport implementations. */
    public Connection() {
    }

    /**
     * It reads a complete packet.
     *
     * @return the packet's bytes, or an empty array if the other side closed
     * @throws ClosedConnectionException if this connection is already closed
     * @throws IOException if the transport fails
     */
    public abstract byte[] readPacket() throws IOException;

    /**
     * It writes a complete packet.
     *
     * @throws ClosedConnectionException if this connection is already closed
     * @throws IllegalArgumentException if {@code pkt} does not even have a JDWP header, or if the
     *     length its header declares does not match the array's
     * @throws IOException if the transport fails
     */
    public abstract void writePacket(byte[] pkt) throws IOException;

    /**
     * It closes the connection.
     *
     * <p>Closing twice is not an error: the second does nothing. A {@link #readPacket} or
     * {@link #writePacket} blocked on another thread is unblocked with
     * {@link ClosedConnectionException}, which is the reason this method exists instead of leaving
     * the work to the collector.
     */
    public abstract void close() throws IOException;

    /** Whether the connection is still open. */
    public abstract boolean isOpen();
}
