package com.sun.jdi;

/**
 * The frame no longer holds: the thread was resumed since it was obtained.
 *
 * @since 1.3
 */
public class InvalidStackFrameException extends RuntimeException {

    /** With no detail. */
    public InvalidStackFrameException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public InvalidStackFrameException(String s) {
        super(s);
    }
}
