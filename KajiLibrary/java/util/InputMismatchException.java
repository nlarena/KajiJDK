package java.util;

// Thrown by a Scanner when the next token does not fit the type asked for.
public class InputMismatchException extends NoSuchElementException {

    public InputMismatchException() {
        super();
    }

    public InputMismatchException(String message) {
        super(message);
    }
}
