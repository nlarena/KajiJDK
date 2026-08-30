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

    // Las dos formas con causa, que llegaron en Java 15. Sirven para lo de siempre: envolver el
    // error de mas abajo sin perderlo.
    public NoSuchElementException(String s, Throwable cause) {
        super(s, cause);
    }

    public NoSuchElementException(Throwable cause) {
        super(cause);
    }
}
