package javax.management;

/**
 * The <b>unchecked</b> root of JMX: what the caller can neither foresee nor handle.
 *
 * <p>The three subclasses that matter wrap something that blew up on the other side --
 * {@link RuntimeMBeanException} a {@code RuntimeException} from the MBean,
 * {@link RuntimeErrorException} an {@code Error}, {@link RuntimeOperationsException} a
 * {@code RuntimeException} from the agent. The wrapper is not ceremony: crossing the MBean server's
 * boundary turns "the MBean has a bug" into a type the client can tell apart from "the agent has
 * a bug".
 */
public class JMRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 6573344628407841861L;

    /** Without a message. */
    public JMRuntimeException() {
        super();
    }

    /** With the message that explains what failed. */
    public JMRuntimeException(String msg) {
        super(msg);
    }

    /**
     * Package-private on purpose, as in the JDK: the subclasses expose the cause through their own
     * field and their {@code getCause()}, not through this constructor.
     */
    JMRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
