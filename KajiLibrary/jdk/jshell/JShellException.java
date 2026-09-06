package jdk.jshell;

/**
 * Something that went wrong on the other side: in the code the user wrote, not in the interpreter.
 *
 * <h2>Why the original exception is not reused</h2>
 *
 * <p>Because the user's code runs on another virtual machine --hence
 * {@code jdk.jshell.execution}-- and an exception from over there is not an object that can be
 * brought across: its class may not exist on this side. What travels is the description, and on this
 * side an {@link EvalException} is built with the original class's name inside.
 *
 * @since 9
 */
public class JShellException extends Exception {

    private static final long serialVersionUID = 1L;

    JShellException() {
        super();
    }

    JShellException(String message) {
        super(message);
    }

    JShellException(String message, Throwable cause) {
        super(message, cause);
    }
}
