package com.sun.jdi.request;

/**
 * The request is not in the state the operation needs.
 *
 * <p>It almost always means the same: a filter was attempted with the request already enabled.
 *
 * @since 1.3
 */
public class InvalidRequestStateException extends RuntimeException {

    /** With no detail. */
    public InvalidRequestStateException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public InvalidRequestStateException(String s) {
        super(s);
    }
}
