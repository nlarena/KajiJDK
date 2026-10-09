package com.sun.nio.sctp;

/**
 * Receiving was asked for over a channel that cannot do it now -- for example a
 * {@link SctpMultiChannel} with no associations.
 */
public class IllegalReceiveException extends IllegalStateException {

    private static final long serialVersionUID = 742758972917L;

    /** With no detail. */
    public IllegalReceiveException() {
        super();
    }

    /** With a message that explains the case. */
    public IllegalReceiveException(String msg) {
        super(msg);
    }
}
