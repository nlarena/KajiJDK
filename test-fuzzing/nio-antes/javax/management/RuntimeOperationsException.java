package javax.management;

/**
 * Wraps a {@code RuntimeException} thrown by <b>the agent</b>, not by the MBean.
 *
 * <p>The typical case is an invalid argument: passing {@code null} where the contract asks for an
 * {@link ObjectName} produces an {@code IllegalArgumentException} wrapped in this. The difference
 * from {@link RuntimeMBeanException} is one of authorship, not of wrapped type: both keep a
 * {@code RuntimeException} and only differ in who threw it.
 */
public class RuntimeOperationsException extends JMRuntimeException {

    private static final long serialVersionUID = -8408923047489133588L;

    /**
     * @serial the wrapped RuntimeException
     */
    private java.lang.RuntimeException runtimeException;

    public RuntimeOperationsException(java.lang.RuntimeException e) {
        super();
        runtimeException = e;
    }

    public RuntimeOperationsException(java.lang.RuntimeException e, String message) {
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
