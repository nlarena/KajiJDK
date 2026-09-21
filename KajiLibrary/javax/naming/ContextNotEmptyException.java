package javax.naming;

/**
 * Thrown when trying to destroy a context that still has things inside. JNDI does not delete in
 * cascade: the caller has to empty it first.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class ContextNotEmptyException extends NamingException {

    private static final long serialVersionUID = 1090963683348219877L;

    public ContextNotEmptyException(String explanation) {
        super(explanation);
    }

    public ContextNotEmptyException() {
        super();
    }
}
