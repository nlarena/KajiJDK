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

    // Se construye directamente a partir del índice ofensor; el mensaje es el del JDK.
    public IndexOutOfBoundsException(int index) {
        super("Index out of range: " + index);
    }

    public IndexOutOfBoundsException(long index) {
        super("Index out of range: " + index);
    }
}
