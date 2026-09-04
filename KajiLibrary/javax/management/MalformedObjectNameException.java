package javax.management;

/** La cadena no respeta la gramatica de ObjectName. */
public class MalformedObjectNameException extends OperationsException {

    private static final long serialVersionUID = -572689714442915824L;

    public MalformedObjectNameException() {
        super();
    }

    public MalformedObjectNameException(String message) {
        super(message);
    }
}
