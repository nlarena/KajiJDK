package java.lang;

// KajiLibrary's java.lang.ArithmeticException — thrown by an integer `/` or `%` with a
// zero divisor.
public class ArithmeticException extends RuntimeException {

    public ArithmeticException() {
    }

    public ArithmeticException(String message) {
        super(message);
    }
}
