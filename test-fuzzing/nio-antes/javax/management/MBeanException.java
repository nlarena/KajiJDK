package javax.management;

/**
 * Wraps an <b>application</b> exception thrown by the MBean.
 *
 * <p>It is the boundary that makes a generic MBean server usable: without it, whoever invokes an
 * operation would receive the MBean's raw exception and could not tell it apart from one of the
 * agent's. With it, {@code MBeanException} always means "your MBean failed" and anything else
 * means "the agent failed".
 *
 * <p>It keeps the wrapped one in a field of its own ({@code exception}) because JMX predates
 * {@code Throwable} chaining; {@link #getCause()} also publishes it the modern way so that stack
 * traces print it.
 */
public class MBeanException extends JMException {

    private static final long serialVersionUID = 4066342430588744142L;

    /**
     * @serial the wrapped application exception
     */
    private java.lang.Exception exception;

    /** Wraps {@code e} without a message of its own. */
    public MBeanException(java.lang.Exception e) {
        super();
        exception = e;
    }

    /** Wraps {@code e} with a message from the agent. */
    public MBeanException(java.lang.Exception e, String message) {
        super(message);
        exception = e;
    }

    /** The wrapped application exception. */
    public java.lang.Exception getTargetException() {
        return exception;
    }

    /** The same as {@link #getTargetException()}, the modern way. */
    public Throwable getCause() {
        return exception;
    }
}
