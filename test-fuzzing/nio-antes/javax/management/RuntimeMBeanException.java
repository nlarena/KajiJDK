package javax.management;

/**
 * Wraps a {@code RuntimeException} thrown by the MBean.
 *
 * <p>It is the unchecked twin of {@link MBeanException}: same meaning --"your MBean failed"-- but
 * for what the MBean does not declare. Wrapping it instead of letting it through is what lets the
 * client know on which side of the boundary it happened.
 */
public class RuntimeMBeanException extends JMRuntimeException {

    private static final long serialVersionUID = 5274912751982730171L;

    /**
     * @serial the wrapped RuntimeException
     */
    private java.lang.RuntimeException runtimeException;

    public RuntimeMBeanException(java.lang.RuntimeException e) {
        super();
        runtimeException = e;
    }

    public RuntimeMBeanException(java.lang.RuntimeException e, String message) {
        super(message);
        runtimeException = e;
    }

    /** The wrapped {@code RuntimeException}. */
    public java.lang.RuntimeException getTargetException() {
        return runtimeException;
    }

    /** The same as {@link #getTargetException()}, the modern way. */
    public Throwable getCause() {
        return runtimeException;
    }
}
