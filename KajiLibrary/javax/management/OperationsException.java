package javax.management;

/**
 * "The request could not be fulfilled, and it is not the MBean's fault."
 *
 * <p>It gathers the ones that report a state of the <b>agent</b>: the name does not exist, the
 * attribute is not there, the listener was not registered. All of them are normal conditions of a
 * management system, not failures.
 */
public class OperationsException extends JMException {

    private static final long serialVersionUID = -4967597595580536216L;

    public OperationsException() {
        super();
    }

    public OperationsException(String message) {
        super(message);
    }
}
