package java.security;

// There is no implementation of the algorithm asked for.
//
// Every `getInstance` factory throws it when no registered provider offers the service. In
// KajiLibrary it is the normal case for almost the whole package: the only factory with real
// algorithms behind it is `MessageDigest`, and only for the three that are implemented from
// scratch.
public class NoSuchAlgorithmException extends GeneralSecurityException {

    public NoSuchAlgorithmException() {
        super();
    }

    public NoSuchAlgorithmException(String message) {
        super(message);
    }

    public NoSuchAlgorithmException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchAlgorithmException(Throwable cause) {
        super(cause);
    }
}
