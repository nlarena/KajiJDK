package javax.naming;

/**
 * Thrown when the thread was interrupted while waiting for the operation to finish. The operation
 * is left in an undefined state: it may or may not have been done.
 *
 * <p>The whole hierarchy and the state it carries are explained in `NamingException`.
 */
public class InterruptedNamingException extends NamingException {

    private static final long serialVersionUID = 6404516648893194728L;

    public InterruptedNamingException(String explanation) {
        super(explanation);
    }

    public InterruptedNamingException() {
        super();
    }
}
