package javax.naming;

/**
 * Thrown when the provider does not implement the requested operation. `Context` is a big
 * interface and some services are read-only or have no subcontexts; this is the agreed way to say
 * so.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class OperationNotSupportedException extends NamingException {

    private static final long serialVersionUID = 5493232822427682064L;

    public OperationNotSupportedException(String explanation) {
        super(explanation);
    }

    public OperationNotSupportedException() {
        super();
    }
}
