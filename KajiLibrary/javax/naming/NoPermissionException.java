package javax.naming;

/**
 * Thrown when the identity is valid and even so the operation is not allowed. It is the
 * difference between "I don't know who you are" and "I know who you are and you can't".
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class NoPermissionException extends NamingSecurityException {

    private static final long serialVersionUID = 8395332708699751775L;

    public NoPermissionException(String explanation) {
        super(explanation);
    }

    public NoPermissionException() {
        super();
    }
}
