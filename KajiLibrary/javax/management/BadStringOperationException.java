package javax.management;

/**
 * A string operation that does not exist was asked for inside a query.
 *
 * <p>It hangs from {@code Exception} and not from {@link JMException}: the four exceptions of the
 * query subsystem sit outside the {@code JMException} tree.
 */
public class BadStringOperationException extends Exception {

    private static final long serialVersionUID = 7802201238441662100L;

    /**
     * @serial the operation that was not recognized
     */
    private String op;

    /** @param message the operation that was not recognized */
    public BadStringOperationException(String message) {
        op = message;
    }

    /** The class name followed by the offending operation. */
    public String toString() {
        return "BadStringOperationException: " + op;
    }
}
