package javax.management;

/** The listener to be removed was not registered. */
public class ListenerNotFoundException extends OperationsException {

    private static final long serialVersionUID = -7242605822448519061L;

    public ListenerNotFoundException() {
        super();
    }

    public ListenerNotFoundException(String message) {
        super(message);
    }
}
