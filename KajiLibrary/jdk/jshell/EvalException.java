package jdk.jshell;

/**
 * The user's code threw an exception.
 *
 * <h2>Why the class name travels as a string</h2>
 *
 * <p>Because the exception was born on another virtual machine and its class may not exist on this
 * side: the user could have declared their own exception in the session. What travels is the name,
 * the message and the stack trace; what arrives is this exception, which belongs to this side.
 *
 * <p>{@link #getCause} returns another {@link EvalException} when the original exception had a
 * cause, wrapped the same way.
 *
 * @since 9
 */
public class EvalException extends JShellException {

    private static final long serialVersionUID = 1L;

    private final String exceptionClass;

    EvalException(String message, String exceptionClass, JShellException cause) {
        super(message, cause);
        this.exceptionClass = exceptionClass;
    }

    /**
     * The name of the original exception's class.
     *
     * @return the class's full name
     */
    public String getExceptionClassName() {
        return this.exceptionClass;
    }

    /**
     * The cause, wrapped like this one.
     *
     * @return the cause, or {@code null}
     */
    @Override
    public JShellException getCause() {
        return (JShellException) super.getCause();
    }
}
