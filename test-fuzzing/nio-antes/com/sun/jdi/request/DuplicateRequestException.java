package com.sun.jdi.request;

/**
 * Something was asked for that was already asked for and does not admit duplicates.
 *
 * <p>It happens with step requests: there may be only one per thread.
 *
 * @since 1.3
 */
public class DuplicateRequestException extends RuntimeException {

    /** With no detail. */
    public DuplicateRequestException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public DuplicateRequestException(String s) {
        super(s);
    }
}
