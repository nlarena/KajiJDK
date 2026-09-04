package javax.management;

/** El oyente que se quiere quitar no estaba registrado. */
public class ListenerNotFoundException extends OperationsException {

    private static final long serialVersionUID = -7242605822448519061L;

    public ListenerNotFoundException() {
        super();
    }

    public ListenerNotFoundException(String message) {
        super(message);
    }
}
