package java.security;

// The key does not serve for what it was asked for: bad encoding, wrong length, an algorithm that
// does not correspond.
public class InvalidKeyException extends KeyException {

    public InvalidKeyException() {
        super();
    }

    public InvalidKeyException(String message) {
        super(message);
    }

    public InvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidKeyException(Throwable cause) {
        super(cause);
    }
}
