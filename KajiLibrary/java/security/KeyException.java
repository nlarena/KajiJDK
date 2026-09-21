package java.security;

// The base of the problems with keys: invalid, badly encoded, impossible to administer.
//
// It is kept as an intermediate level —and is not collapsed against `GeneralSecurityException`—
// because `InvalidKeyException` and `KeyManagementException` hang from it, and there is code that
// wants to catch "anything about keys" without also catching a signature failure.
public class KeyException extends GeneralSecurityException {

    public KeyException() {
        super();
    }

    public KeyException(String message) {
        super(message);
    }

    public KeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyException(Throwable cause) {
        super(cause);
    }
}
