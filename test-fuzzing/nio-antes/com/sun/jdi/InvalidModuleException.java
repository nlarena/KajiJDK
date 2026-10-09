package com.sun.jdi;

/**
 * InvalidModuleException of the debugged machine.
 *
 * @since 1.3
 */
public class InvalidModuleException extends RuntimeException {

    /** With no detail. */
    public InvalidModuleException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public InvalidModuleException(String s) {
        super(s);
    }
}
