package com.sun.jdi;

/**
 * The class is loaded but not prepared yet.
 *
 * <p>Between loading and preparing there is a step: the VM has to link the class and give its
 * static fields a value. Before that there is no state to read.
 *
 * @since 1.3
 */
public class ClassNotPreparedException extends RuntimeException {

    /** With no detail. */
    public ClassNotPreparedException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public ClassNotPreparedException(String s) {
        super(s);
    }
}
