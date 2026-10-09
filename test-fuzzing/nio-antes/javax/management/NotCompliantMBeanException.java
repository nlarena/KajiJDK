package javax.management;

/** The class does not follow any of the MBean patterns. */
public class NotCompliantMBeanException extends OperationsException {

    private static final long serialVersionUID = 5175579583207963577L;

    public NotCompliantMBeanException() {
        super();
    }

    public NotCompliantMBeanException(String message) {
        super(message);
    }
}
