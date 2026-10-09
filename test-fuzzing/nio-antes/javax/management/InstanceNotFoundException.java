package javax.management;

/** No MBean is registered with that ObjectName. */
public class InstanceNotFoundException extends OperationsException {

    private static final long serialVersionUID = -882579438394773049L;

    public InstanceNotFoundException() {
        super();
    }

    public InstanceNotFoundException(String message) {
        super(message);
    }
}
