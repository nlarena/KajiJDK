package com.sun.jdi;

/**
 * The class does not carry the debugging information that was asked for.
 *
 * <p>The local variables' names and the line numbers are optional: they are emitted only if it
 * was compiled asking for them. Receiving this does not mean that the program is wrong.
 *
 * @since 1.3
 */
public class AbsentInformationException extends Exception {

    /** With no detail. */
    public AbsentInformationException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public AbsentInformationException(String s) {
        super(s);
    }
}
