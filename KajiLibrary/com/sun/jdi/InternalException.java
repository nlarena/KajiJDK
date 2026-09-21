package com.sun.jdi;

/**
 * InternalException of the debugged machine.
 *
 * @since 1.3
 */
public class InternalException extends RuntimeException {

    private final int errorCode;

    /** With no detail. */
    public InternalException() {
        super();
        this.errorCode = 0;
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public InternalException(String s) {
        super(s);
        this.errorCode = 0;
    }

    /**
     * With the transport layer's error code.
     *
     * @param errorCode the code
     */
    public InternalException(int errorCode) {
        super();
        this.errorCode = errorCode;
    }

    /**
     * With a message and the error code.
     *
     * @param s the message
     * @param errorCode the code
     */
    public InternalException(String s, int errorCode) {
        super(s);
        this.errorCode = errorCode;
    }

    /**
     * The transport layer's error code, or zero if there was none.
     *
     * @return the code
     */
    public int errorCode() {
        return errorCode;
    }
}
