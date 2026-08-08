package java.lang;

// KajiLibrary's java.lang.ArrayIndexOutOfBoundsException — thrown when an array is
// indexed with a value outside `[0, length)`.
public class ArrayIndexOutOfBoundsException extends RuntimeException {

    public ArrayIndexOutOfBoundsException() {
    }

    public ArrayIndexOutOfBoundsException(String message) {
        super(message);
    }
}
