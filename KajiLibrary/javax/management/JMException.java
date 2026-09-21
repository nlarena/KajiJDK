package javax.management;

/**
 * The <b>checked</b> root of JMX: everything a management operation can fail with and the caller
 * has to handle hangs from here.
 *
 * <p>JMX splits its errors into two trees that do <b>not</b> touch: this one, which extends
 * {@code Exception}, and {@link JMRuntimeException}, which extends {@code RuntimeException}. The
 * split is not one of convenience but of responsibility -- the checked one says "the request could
 * not be fulfilled" (the MBean does not exist, the attribute is not there) and the unchecked one
 * says "the MBean broke or was called wrongly".
 *
 * <p>No constructor with a cause, and not by oversight: JMX predates {@code Throwable} chaining.
 * The two subclasses that do wrap something --{@link MBeanException} and
 * {@link ReflectionException}-- keep it in a field of their own and publish it by overriding
 * {@code getCause()}.
 */
public class JMException extends Exception {

    private static final long serialVersionUID = 350520924977331825L;

    /** Without a message. */
    public JMException() {
        super();
    }

    /** With the message that explains what failed. */
    public JMException(String msg) {
        super(msg);
    }
}
