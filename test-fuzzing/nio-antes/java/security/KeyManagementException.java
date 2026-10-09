package java.security;

// A failure in the handling of keys seen from outside a concrete operation: a store that could not
// be opened, a key that could not be published or revoked.
//
// It hangs from `KeyException` and not from `GeneralSecurityException` because the subject is still
// the key; what changes is that the problem is one of administration and not of use.
public class KeyManagementException extends KeyException {

    public KeyManagementException() {
        super();
    }

    public KeyManagementException(String message) {
        super(message);
    }

    public KeyManagementException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyManagementException(Throwable cause) {
        super(cause);
    }
}
