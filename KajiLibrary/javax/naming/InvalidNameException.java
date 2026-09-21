package javax.naming;

/**
 * Thrown when a name does not respect the syntax of the namespace it is sent to.
 *
 * <p>It can be thrown without any service involved: `CompoundName` and `CompositeName` parsing
 * throw it, and so does `addAll` when given a name of another class or when a second component is
 * added to a flat name. (An earlier note called it the only one of the family thrown that way; it
 * is not -- `LinkRef.getLinkName` throws `MalformedLinkException` and `InitialContext` throws
 * `NoInitialContextException` without any service either.)
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class InvalidNameException extends NamingException {

    private static final long serialVersionUID = -8370672380823801105L;

    public InvalidNameException(String explanation) {
        super(explanation);
    }

    public InvalidNameException() {
        super();
    }
}
