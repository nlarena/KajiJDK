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
}
