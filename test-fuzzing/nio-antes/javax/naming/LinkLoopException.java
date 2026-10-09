package javax.naming;

/**
 * Thrown when following links passes through the same point again, or when the hop limit was
 * exceeded. Without this, a link pointing to itself would hang whoever resolves it.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class LinkLoopException extends LinkException {

    private static final long serialVersionUID = -3119189944325198009L;

    public LinkLoopException(String explanation) {
        super(explanation);
    }

    public LinkLoopException() {
        super();
    }
}
