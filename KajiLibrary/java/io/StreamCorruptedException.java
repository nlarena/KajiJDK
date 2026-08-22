package java.io;

import java.io.ObjectStreamException;

// The stream's own framing is broken: a bad magic number, an unknown type code, a block
// length that runs past the data. Unlike InvalidObjectException this is not about any
// particular object's invariants — nothing further can be read at all.
public class StreamCorruptedException extends ObjectStreamException {

    public StreamCorruptedException() {
    }

    public StreamCorruptedException(String message) {
        super(message);
    }
}
