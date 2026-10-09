package java.io;

// An Error, not an Exception: raised for I/O failures so severe that no reasonable program
// could recover from them (the JDK uses it for the Console, whose stream is the process's
// own terminal). The distinction is about recoverability, not about where the failure came
// from — an ordinary unreadable file is an IOException, a broken stdin is an IOError. Note
// it takes only a cause: an IOError is always a rewrapping of some lower-level failure.
public class IOError extends Error {

    public IOError(Throwable cause) {
        super(cause);
    }
}
