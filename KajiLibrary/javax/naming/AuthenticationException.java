package javax.naming;

/**
 * Thrown when the naming service rejected the identity: wrong, expired or missing credentials.
 * Retrying with the same ones does not help; you need different ones.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class AuthenticationException extends NamingSecurityException {

    private static final long serialVersionUID = 3678497619904568096L;

    public AuthenticationException(String explanation) {
        super(explanation);
    }

    public AuthenticationException() {
        super();
    }
}
