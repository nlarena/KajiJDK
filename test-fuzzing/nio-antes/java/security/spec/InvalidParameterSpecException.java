package java.security.spec;

import java.security.GeneralSecurityException;

// The parameter spec given does not match the algorithm.
public class InvalidParameterSpecException extends GeneralSecurityException {

    public InvalidParameterSpecException() {
        super();
    }

    public InvalidParameterSpecException(String message) {
        super(message);
    }
}
