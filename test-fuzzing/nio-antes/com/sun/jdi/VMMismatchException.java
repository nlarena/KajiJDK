package com.sun.jdi;

/**
 * VMMismatchException of the debugged machine.
 *
 * @since 1.3
 */
public class VMMismatchException extends RuntimeException {

    /** With no detail. */
    public VMMismatchException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public VMMismatchException(String s) {
        super(s);
    }
}
