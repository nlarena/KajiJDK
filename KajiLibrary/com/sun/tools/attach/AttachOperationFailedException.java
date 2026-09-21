package com.sun.tools.attach;

import java.io.IOException;

/**
 * The target VM received the operation, executed it and answered that it failed.
 *
 * <p>It is an {@link IOException} and there is the shade of meaning that is worth it: it
 * inherits from the same class as the communication failures, but it means the opposite. A
 * common {@code IOException} means that the channel broke and what happened on the other side
 * is not known; this one means that the channel worked perfectly and the answer was "no". The
 * message comes from the target VM, not from this one.
 */
public class AttachOperationFailedException extends IOException {

    private static final long serialVersionUID = 2140308168167478043L;

    /** With the message the target VM sent. */
    public AttachOperationFailedException(String message) {
        super(message);
    }
}
