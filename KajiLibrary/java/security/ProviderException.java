package java.security;

// A cryptographic provider failed inside.
//
// It is **unchecked** on purpose, and it is the only one of the package that is so without being an
// error of argument: it points out that the implementation is broken —not that the caller asked for
// something impossible— and forcing it to be declared in every signature would give nobody a way of
// recovering. Whoever catches it can do nothing but abort.
public class ProviderException extends RuntimeException {

    public ProviderException() {
        super();
    }

    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderException(Throwable cause) {
        super(cause);
    }
}
