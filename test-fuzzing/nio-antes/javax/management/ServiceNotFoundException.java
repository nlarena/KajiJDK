package javax.management;

/** The requested service does not exist or could not be applied. */
public class ServiceNotFoundException extends OperationsException {

    private static final long serialVersionUID = -3990675661956646827L;

    public ServiceNotFoundException() {
        super();
    }

    public ServiceNotFoundException(String message) {
        super(message);
    }
}
