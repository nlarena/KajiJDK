package javax.management;

/**
 * Wraps an {@code Error} that came out of the MBean.
 *
 * <p>It exists for a reason of types, not of diagnosis: an {@code Error} is not an
 * {@code Exception}, so {@link MBeanException} cannot carry it. Without this wrapper an
 * {@code OutOfMemoryError} from the MBean would come up raw through the agent and would be
 * indistinguishable from one of the agent itself.
 */
public class RuntimeErrorException extends JMRuntimeException {

    private static final long serialVersionUID = 704338937753949796L;

    /**
     * @serial the wrapped Error
     */
    private java.lang.Error error;

    public RuntimeErrorException(java.lang.Error e) {
        super();
        error = e;
    }

    public RuntimeErrorException(java.lang.Error e, String message) {
        super(message);
        error = e;
    }

    /** The wrapped {@code Error}. */
    public java.lang.Error getTargetError() {
        return error;
    }

    /** The same as {@link #getTargetError()}, the modern way. */
    public Throwable getCause() {
        return error;
    }
}
