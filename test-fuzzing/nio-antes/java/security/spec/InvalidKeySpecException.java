package java.security.spec;

import java.security.GeneralSecurityException;

// The key spec given is not valid for what was asked to be done with it.
public class InvalidKeySpecException extends GeneralSecurityException {

    public InvalidKeySpecException() {
        super();
    }

    public InvalidKeySpecException(String message) {
        super(message);
    }

    public InvalidKeySpecException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidKeySpecException(Throwable cause) {
        super(cause);
    }
}
