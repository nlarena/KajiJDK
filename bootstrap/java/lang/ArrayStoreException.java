package java.lang;

// Minimal ArrayStoreException — thrown by `aastore` when the stored value is not
// assignable to the array's element type (JVMS §6.5, array covariance's runtime check).
public class ArrayStoreException extends RuntimeException {
    public ArrayStoreException(String message) {
        super(message);
    }

    public ArrayStoreException() {
    }
}
