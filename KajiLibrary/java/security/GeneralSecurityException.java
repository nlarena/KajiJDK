package java.security;

// The root of the checked exceptions of the package.
//
// It exists so that a caller who does not want to tell "the algorithm is not there", "the key does
// not serve" and "the signature could not be processed" apart can catch all three with a single
// catch. Almost the whole package throws one of its subclasses, and the two that step out of the
// hierarchy do it on purpose: `ProviderException` is unchecked because it points at a broken
// provider, and `InvalidParameterException` because it points at a programming error of the
// caller.
public class GeneralSecurityException extends Exception {

    public GeneralSecurityException() {
        super();
    }

    public GeneralSecurityException(String message) {
        super(message);
    }

    public GeneralSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeneralSecurityException(Throwable cause) {
        super(cause);
    }
}
