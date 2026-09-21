package com.sun.jdi;

/**
 * InvalidLineNumberException of the debugged machine.
 *
 * @since 1.3
 */
public class InvalidLineNumberException extends RuntimeException {

    /** With no detail. */
    public InvalidLineNumberException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public InvalidLineNumberException(String s) {
        super(s);
    }
}
