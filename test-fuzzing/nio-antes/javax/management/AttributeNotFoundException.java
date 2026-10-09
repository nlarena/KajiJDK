package javax.management;

/** The requested attribute does not exist in the MBean. */
public class AttributeNotFoundException extends OperationsException {

    private static final long serialVersionUID = 6511584241791106926L;

    public AttributeNotFoundException() {
        super();
    }

    public AttributeNotFoundException(String message) {
        super(message);
    }
}
