package javax.naming;

/**
 * Thrown when the service was reached and the service said it is not available. Compared with
 * `CommunicationException`, here the channel worked: what is missing is the service.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class ServiceUnavailableException extends NamingException {

    private static final long serialVersionUID = -4996964726566773444L;

    public ServiceUnavailableException(String explanation) {
        super(explanation);
    }

    public ServiceUnavailableException() {
        super();
    }
}
