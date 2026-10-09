package com.sun.jdi;

/**
 * InvalidTypeException of the debugged machine.
 *
 * @since 1.3
 */
public class InvalidTypeException extends Exception {

    /** With no detail. */
    public InvalidTypeException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public InvalidTypeException(String s) {
        super(s);
    }
}
