package java.lang.reflect;

/**
 * Thrown when {@code setAccessible} cannot suppress access checking for a member.
 *
 * <p>This is the module system speaking. Before modules, {@code setAccessible(true)} always
 * succeeded and reflection could reach anything; now a package must be OPEN to the caller's module
 * for that to be allowed. The exception is what "strong encapsulation" looks like from the calling
 * side — a refusal that no permission check or accessibility flag can talk its way past.
 */
public class InaccessibleObjectException extends RuntimeException {

    /**
     * Creates an exception with no detail message.
     */
    public InaccessibleObjectException() {
        super();
    }

    /**
     * Creates an exception with the given detail message.
     *
     * @param msg the detail message
     */
    public InaccessibleObjectException(String msg) {
        super(msg);
    }
}
