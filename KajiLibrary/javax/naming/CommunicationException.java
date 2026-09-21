package javax.naming;

/**
 * Thrown when communication with the naming service failed: dropped connection, broken protocol,
 * unreadable reply. The concrete cause usually comes chained as the root cause.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class CommunicationException extends NamingException {

    private static final long serialVersionUID = 3618507780299986611L;

    public CommunicationException(String explanation) {
        super(explanation);
    }

    public CommunicationException() {
        super();
    }
}
