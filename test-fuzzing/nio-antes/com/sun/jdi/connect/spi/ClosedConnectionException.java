package com.sun.jdi.connect.spi;

import java.io.IOException;

/**
 * The connection closed or broke while it was being used.
 *
 * <p>It is an {@link IOException} and not something of its own because for whoever receives it
 * that is exactly what it is: an input/output operation that could not be completed. What it
 * adds over any {@code IOException} is the <em>cause</em>, and it is a distinction that matters
 * -- an orderly end of stream is reported with a packet of length zero from
 * {@link Connection#readPacket}, not with this exception. Seeing it means that the connection
 * no longer serves, not that the other side has finished talking.
 */
public class ClosedConnectionException extends IOException {

    private static final long serialVersionUID = 3877032124297204774L;

    /** With no detail. */
    public ClosedConnectionException() {
        super();
    }

    /** With a message that explains what closed it. */
    public ClosedConnectionException(String message) {
        super(message);
    }
}
