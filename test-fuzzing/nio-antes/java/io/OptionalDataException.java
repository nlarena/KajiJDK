package java.io;

import java.io.ObjectStreamException;

// Thrown by readObject when the next thing in the stream is not an object but primitive data
// (or the end of the object's data). It is really a control signal dressed as an exception:
// the two public fields say how many bytes of primitive data are waiting (`length`) or that
// the object's data is exhausted (`eof`), so the reader knows how to continue rather than
// merely that it failed. Its constructors are package-private in the JDK — only the
// serialization machinery is entitled to raise it — and are kept package-private here.
public class OptionalDataException extends ObjectStreamException {

    public int length;
    public boolean eof;

    OptionalDataException(int len) {
        this.length = len;
        this.eof = false;
    }

    OptionalDataException(boolean end) {
        this.length = 0;
        this.eof = end;
    }
}
