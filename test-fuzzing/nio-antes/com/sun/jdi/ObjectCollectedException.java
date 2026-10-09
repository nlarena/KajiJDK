package com.sun.jdi;

/**
 * The object on the other side no longer exists: the collector took it away.
 *
 * <p>It is the exception that makes it visible that these objects are <strong>mirrors</strong>.
 * Between obtaining a reference and using it, the other VM goes on running. In order to avoid
 * that there is {@code ObjectReference.disableCollection}.
 *
 * @since 1.3
 */
public class ObjectCollectedException extends RuntimeException {

    /** With no detail. */
    public ObjectCollectedException() {
        super();
    }

    /**
     * With a message.
     *
     * @param s the message
     */
    public ObjectCollectedException(String s) {
        super(s);
    }
}
