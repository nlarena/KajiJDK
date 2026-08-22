package java.io;

import java.io.IOException;

// The abstract root of everything object serialization can go wrong with. It is abstract and
// its constructors are protected because it is a category, not a diagnosis: code catches
// ObjectStreamException to mean "the object stream is unusable", and the concrete subclass
// says why. KajiLibrary has no serialization engine, but the exception vocabulary is pure
// Java and pins down the contract the engine would have to honour.
public abstract class ObjectStreamException extends IOException {

    protected ObjectStreamException() {
    }

    protected ObjectStreamException(String message) {
        super(message);
    }

    protected ObjectStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    protected ObjectStreamException(Throwable cause) {
        super(cause);
    }
}
