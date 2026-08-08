package java.lang;

// KajiLibrary's java.lang.NegativeArraySizeException — thrown when an array is created
// with a negative length.
public class NegativeArraySizeException extends RuntimeException {

    public NegativeArraySizeException() {
    }

    public NegativeArraySizeException(String message) {
        super(message);
    }
}
