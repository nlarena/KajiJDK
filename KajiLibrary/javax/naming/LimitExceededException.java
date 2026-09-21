package javax.naming;

/**
 * Thrown when the operation was cut short by an agreed limit --on results or on time-- and not
 * by an error.
 *
 * <p>It is not abstract, unlike `NamingSecurityException`: a provider may have hit a limit that is
 * neither size nor time, and then it throws this one. What arrives is **partial**, not empty.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class LimitExceededException extends NamingException {

    private static final long serialVersionUID = -776898738660207856L;

    public LimitExceededException(String explanation) {
        super(explanation);
    }

    public LimitExceededException() {
        super();
    }
}
