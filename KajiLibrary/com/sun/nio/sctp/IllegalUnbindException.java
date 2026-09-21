package com.sun.nio.sctp;

/**
 * Unbinding an address that cannot be unbound was asked for: the last one that is left, or one
 * that was never bound.
 */
public class IllegalUnbindException extends IllegalStateException {

    private static final long serialVersionUID = 2493124086598L;

    /** With no detail. */
    public IllegalUnbindException() {
        super();
    }

    /** With a message that explains the case. */
    public IllegalUnbindException(String msg) {
        super(msg);
    }
}
