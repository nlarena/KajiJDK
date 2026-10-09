package java.io;

import java.io.ObjectStreamException;

// Thrown when an object graph being written reaches a class that does not implement
// Serializable. It surfaces at write time rather than compile time because serializability
// is a property of the whole reachable graph, which only the runtime can see: a Serializable
// class with one non-Serializable field still fails here.
public class NotSerializableException extends ObjectStreamException {

    public NotSerializableException() {
    }

    public NotSerializableException(String message) {
        super(message);
    }
}
