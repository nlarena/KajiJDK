package java.security;

// The parameters passed to the algorithm are not the ones the algorithm accepts.
//
// Not to be confused with `InvalidParameterException`, which is unchecked and inherits from
// `IllegalArgumentException`: that one points at a programming error of the caller, this one points
// at a parameter/algorithm combination that can only be discovered at runtime.
public class InvalidAlgorithmParameterException extends GeneralSecurityException {

    public InvalidAlgorithmParameterException() {
        super();
    }

    public InvalidAlgorithmParameterException(String message) {
        super(message);
    }

    public InvalidAlgorithmParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAlgorithmParameterException(Throwable cause) {
        super(cause);
    }
}
