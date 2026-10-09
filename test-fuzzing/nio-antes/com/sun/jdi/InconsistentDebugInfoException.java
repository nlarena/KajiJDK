package com.sun.jdi;

/**
 * InconsistentDebugInfoException of the debugged machine.
 *
 * @since 1.3
 */
public class InconsistentDebugInfoException extends RuntimeException {

    /** With no detail. */
    public InconsistentDebugInfoException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public InconsistentDebugInfoException(String s) {
        super(s);
    }
}
