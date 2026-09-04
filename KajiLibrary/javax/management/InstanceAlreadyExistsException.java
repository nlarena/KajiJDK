package javax.management;

/** Ya hay un MBean registrado con ese ObjectName. */
public class InstanceAlreadyExistsException extends OperationsException {

    private static final long serialVersionUID = 8893743928912733931L;

    public InstanceAlreadyExistsException() {
        super();
    }

    public InstanceAlreadyExistsException(String message) {
        super(message);
    }
}
