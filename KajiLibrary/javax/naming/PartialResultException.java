package javax.naming;

/**
 * Thrown when the operation returned an incomplete result because it could not carry on --for
 * example, part of the tree lives on another server that could not be reached. What was already
 * delivered is usable; what is missing is unknown.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class PartialResultException extends NamingException {

    private static final long serialVersionUID = 2572144970049426786L;

    public PartialResultException(String explanation) {
        super(explanation);
    }

    public PartialResultException() {
        super();
    }
}
