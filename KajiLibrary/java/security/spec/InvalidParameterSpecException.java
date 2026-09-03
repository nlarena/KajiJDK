package java.security.spec;

import java.security.GeneralSecurityException;

// La spec de parametros que se dio no corresponde al algoritmo.
public class InvalidParameterSpecException extends GeneralSecurityException {

    public InvalidParameterSpecException() {
        super();
    }

    public InvalidParameterSpecException(String message) {
        super(message);
    }
}
