package java.util;

// KajiLibrary's java.util.NoSuchElementException — thrown when an accessor is asked for an
// element that isn't there (an empty Optional's get(), an exhausted Iterator's next()).
public class NoSuchElementException extends RuntimeException {

    public NoSuchElementException() {
        super();
    }

    public NoSuchElementException(String s) {
        super(s);
    }

    // The two forms with a cause, which arrived in Java 15. They serve the usual purpose: wrapping
    // the error from further down without losing it.
    public NoSuchElementException(String s, Throwable cause) {
        super(s, cause);
    }

    public NoSuchElementException(Throwable cause) {
        super(cause);
    }
}
