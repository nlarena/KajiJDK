package javax.management;

/** La clase no cumple ninguno de los patrones de MBean. */
public class NotCompliantMBeanException extends OperationsException {

    private static final long serialVersionUID = 5175579583207963577L;

    public NotCompliantMBeanException() {
        super();
    }

    public NotCompliantMBeanException(String message) {
        super(message);
    }
}
