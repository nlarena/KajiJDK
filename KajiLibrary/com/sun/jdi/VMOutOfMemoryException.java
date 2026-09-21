package com.sun.jdi;

/**
 * VMOutOfMemoryException of the debugged machine.
 *
 * @since 1.3
 */
public class VMOutOfMemoryException extends RuntimeException {

    /** With no detail. */
    public VMOutOfMemoryException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public VMOutOfMemoryException(String s) {
        super(s);
    }
}
