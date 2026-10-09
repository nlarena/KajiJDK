package java.io;

import java.io.ObjectStreamException;

// Thrown when one of the serialization callbacks (defaultWriteObject, registerValidation,
// and friends) is called outside the writeObject/readObject call it only makes sense
// inside. Those methods act on an implicit "currently active" object, so calling one at any
// other time has no object to act on.
public class NotActiveException extends ObjectStreamException {

    public NotActiveException() {
    }

    public NotActiveException(String message) {
        super(message);
    }
}
