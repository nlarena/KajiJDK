package java.util;

import java.io.IOException;

// A `Properties`'s XML does not satisfy the properties DTD.
//
// It is an `IOException` and not a `RuntimeException` because whoever loads a properties file is
// already obliged to deal with I/O: a malformed file is another way for the load to fail, not a
// separate category the caller has to discover.
//
// The JDK declares it without the `Throwable` constructors that accept a null cause; that shape is
// replicated: the two that are here, and no more.
public class InvalidPropertiesFormatException extends IOException {

    // With `cause` as the cause; the message comes from it.
    public InvalidPropertiesFormatException(Throwable cause) {
        super(cause == null ? null : cause.toString());
        this.initCause(cause);
    }

    // With the given message.
    public InvalidPropertiesFormatException(String message) {
        super(message);
    }
}
