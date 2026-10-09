package javax.naming;

/**
 * Thrown when the requested authentication mechanism is not supported by the service --or not
 * supported for **this** operation. Unlike `AuthenticationException`, the credentials may be
 * perfect: what is not accepted is the way they are presented.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class AuthenticationNotSupportedException extends NamingSecurityException {

    private static final long serialVersionUID = -7149033933259492300L;

    public AuthenticationNotSupportedException(String explanation) {
        super(explanation);
    }

    public AuthenticationNotSupportedException() {
        super();
    }
}
