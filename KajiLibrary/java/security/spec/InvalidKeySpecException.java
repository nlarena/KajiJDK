package java.security.spec;

import java.security.GeneralSecurityException;

// La spec de clave que se dio no es valida para lo que se pidio hacer con ella.
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
