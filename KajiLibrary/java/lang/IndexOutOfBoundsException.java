package java.lang;

// Thrown when an index into a sequence — an array, a string, a list — is negative or not
// less than the sequence's size.
public class IndexOutOfBoundsException extends RuntimeException {

    public IndexOutOfBoundsException() {
        super();
    }

    public IndexOutOfBoundsException(String message) {
        super(message);
    }
}
