package java.security;

// An invalid parameter passed to a method of the package.
//
// It is one of the few of the package that does **not** inherit from `GeneralSecurityException`: it
// inherits from `IllegalArgumentException`, and that is not a historical accident but the useful
// distinction. An invalid argument is an error of the caller and is fixed by changing the code, so
// there is no reason for it to be checked. An absent algorithm or a signature that cannot be
// processed are states of the world, and those are.
public class InvalidParameterException extends IllegalArgumentException {

    public InvalidParameterException() {
        super();
    }

    public InvalidParameterException(String message) {
        super(message);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidParameterException(Throwable cause) {
        super(cause);
    }
}
