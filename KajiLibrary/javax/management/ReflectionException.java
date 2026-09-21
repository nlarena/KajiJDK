package javax.management;

/**
 * Wraps an exception from {@code java.lang.reflect} that came out while constructing or invoking
 * by reflection.
 *
 * <p>It is told apart from {@link MBeanException} precisely by where things broke: here the
 * <b>access</b> failed (the class does not exist, the method does not exist, it cannot be
 * accessed), not the MBean's body. A {@code ClassNotFoundException} when creating an MBean is this;
 * an {@code IllegalStateException} thrown inside its constructor is an {@code MBeanException}.
 */
public class ReflectionException extends JMException {

    private static final long serialVersionUID = 9170809325636915553L;

    /**
     * @serial the wrapped reflection exception
     */
    private java.lang.Exception exception;

    public ReflectionException(java.lang.Exception e) {
        super();
        exception = e;
    }

    public ReflectionException(java.lang.Exception e, String message) {
        super(message);
        exception = e;
    }

    /** The wrapped reflection exception. */
    public java.lang.Exception getTargetException() {
        return exception;
    }

    /** The same as {@link #getTargetException()}, the modern way. */
    public Throwable getCause() {
        return exception;
    }
}
