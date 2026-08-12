package java.lang;

// Thrown to indicate that a method was passed an illegal or inappropriate argument.
public class IllegalArgumentException extends RuntimeException {
    public IllegalArgumentException(String message) {
        super(message);
    }

    public IllegalArgumentException() {
    }
}
