package java.lang;

// Minimal NegativeArraySizeException — thrown when `newarray`/`anewarray` gets a
// negative length.
public class NegativeArraySizeException extends RuntimeException {
    public NegativeArraySizeException(String message) {
        super(message);
    }

    public NegativeArraySizeException() {
    }
}
