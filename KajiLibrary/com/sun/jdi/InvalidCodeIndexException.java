package com.sun.jdi;

/**
 * InvalidCodeIndexException of the debugged machine.
 *
 * @since 1.3
 */
public class InvalidCodeIndexException extends RuntimeException {

    /** With no detail. */
    public InvalidCodeIndexException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public InvalidCodeIndexException(String s) {
        super(s);
    }
}
