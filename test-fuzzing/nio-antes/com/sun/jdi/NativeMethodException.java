package com.sun.jdi;

/**
 * NativeMethodException of the debugged machine.
 *
 * @since 1.3
 */
public class NativeMethodException extends OpaqueFrameException {

    /** With no detail. */
    public NativeMethodException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public NativeMethodException(String s) {
        super(s);
    }
}
