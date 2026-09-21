package com.sun.jdi;

/**
 * The operation needs the thread suspended and the thread is running.
 *
 * <p>Reading the stack of a moving thread does not give an incomplete result: it gives a
 * meaningless one, because the frames change while they are being walked.
 *
 * @since 1.3
 */
public class IncompatibleThreadStateException extends Exception {

    /** With no detail. */
    public IncompatibleThreadStateException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public IncompatibleThreadStateException(String s) {
        super(s);
    }
}
