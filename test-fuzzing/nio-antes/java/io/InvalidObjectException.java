package java.io;

import java.io.ObjectStreamException;

// Thrown by an object's own validation when the deserialized state is not a legal state for
// that class. Deserialization bypasses constructors, so invariants a constructor would have
// enforced can arrive violated; this is how a class rejects such an object instead of
// letting a corrupt instance escape.
public class InvalidObjectException extends ObjectStreamException {

    public InvalidObjectException(String message) {
        super(message);
    }

    public InvalidObjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
