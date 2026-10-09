package com.sun.jdi;

/**
 * The method that was invoked on the other side threw an exception.
 *
 * <p>{@link #exception} returns the exception <strong>of the other VM</strong>, as an
 * {@link ObjectReference}. It cannot be thrown here nor converted: it is an object of another
 * process.
 *
 * @since 1.3
 */
public class InvocationException extends Exception {

    private final ObjectReference exception;

    /**
     * With the exception the method on the other side threw.
     *
     * <p>The message is fixed, as in the JDK: the detail is in the exception itself, which lives
     * in the other VM and cannot be formatted from here.
     *
     */
    public InvocationException(ObjectReference exception) {
        super("Exception occurred in target VM");
        this.exception = exception;
    }

    /**
     * The exception the method threw, as an object of the other VM.
     *
     * @return the exception
     */
    public ObjectReference exception() {
        return exception;
    }
}
