package com.sun.jdi;

/**
 * The connection with the debugged machine was cut off.
 *
 * <p>It is unchecked on purpose: it may happen in any JDI call, and declaring it in all of them
 * would force every line of a debugger to be wrapped in a {@code try}.
 *
 * @since 1.3
 */
public class VMDisconnectedException extends RuntimeException {

    /** With no detail. */
    public VMDisconnectedException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public VMDisconnectedException(String s) {
        super(s);
    }
}
