package com.sun.nio.sctp;

/**
 * A {@link MessageInfo}'s stream number is outside the range the association negotiated.
 *
 * <p>It is an {@link IllegalArgumentException} and not an I/O one because the error is the
 * program's, not the network's: the range is known beforehand in
 * {@link Association#maxOutboundStreams}.
 */
public class InvalidStreamException extends IllegalArgumentException {

    private static final long serialVersionUID = 29332933412071L;

    /** With no detail. */
    public InvalidStreamException() {
        super();
    }

    /** With a message that explains the case. */
    public InvalidStreamException(String msg) {
        super(msg);
    }
}
