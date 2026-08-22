package java.io;

// KajiLibrary's java.io.IOException — the checked exception every I/O operation in this
// package signals failure with. It is checked (not a RuntimeException) on purpose: an
// I/O failure is an expected outcome of talking to the outside world, not a program bug,
// so the compiler forces callers to decide what to do about it.
//
// A note on this package: our stream classes do not yet DECLARE `throws IOException`,
// because the abstract bases (InputStream/Reader/Writer/Closeable) were written without
// it and Java forbids an override from widening the checked exceptions of the method it
// overrides. The type exists so the exceptions below have a common root and so code can
// throw and catch it; threading the declaration through the hierarchy is a separate,
// mechanical change.
public class IOException extends Exception {

    public IOException() {
    }

    public IOException(String message) {
        super(message);
    }

    public IOException(String message, Throwable cause) {
        super(message, cause);
    }

    public IOException(Throwable cause) {
        super(cause);
    }
}
