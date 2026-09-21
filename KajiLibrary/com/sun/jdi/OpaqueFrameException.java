package com.sun.jdi;

/**
 * The frame does not allow that operation.
 *
 * <p>It happens with native frames and with those of a mounted virtual thread: there is no Java
 * stack to manipulate.
 *
 * @since 1.3
 */
public class OpaqueFrameException extends RuntimeException {

    /** With no detail. */
    public OpaqueFrameException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public OpaqueFrameException(String s) {
        super(s);
    }
}
