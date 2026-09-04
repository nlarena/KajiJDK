package javax.management;

/** Fallo la introspeccion del MBean. */
public class IntrospectionException extends OperationsException {

    private static final long serialVersionUID = 1054516935875481725L;

    public IntrospectionException() {
        super();
    }

    public IntrospectionException(String message) {
        super(message);
    }
}
