package com.sun.jdi;

/**
 * The debugged machine is in read-only mode.
 *
 * <p>A debugger that only observes may be connected, and there everything that changes the
 * other side's state -- writing a field, invoking a method -- fails with this.
 *
 * @since 1.3
 */
public class VMCannotBeModifiedException extends UnsupportedOperationException {

    /** With no detail. */
    public VMCannotBeModifiedException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public VMCannotBeModifiedException(String s) {
        super(s);
    }
}
