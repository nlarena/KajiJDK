package java.lang.reflect;

/**
 * Thrown by a proxy instance when its invocation handler throws a checked exception that the proxied
 * method does not declare.
 *
 * <p>It exists to keep a hole out of the type system. A proxy must satisfy the interface it stands
 * for, {@code throws} clause included, but its handler is written against a signature that permits
 * any {@link Throwable}. When the handler throws something the interface never declared, the proxy
 * cannot let it through unchanged without making the checked-exception contract a lie — so it
 * wraps it in this unchecked exception instead.
 */
public class UndeclaredThrowableException extends RuntimeException {

    private final Throwable undeclaredThrowable;

    /**
     * Creates an exception wrapping the given throwable.
     *
     * @param undeclaredThrowable the throwable the handler threw
     */
    public UndeclaredThrowableException(Throwable undeclaredThrowable) {
        // Passed to super as the CAUSE rather than exposed through an overridden getCause(): the
        // JDK does not declare getCause() here, so declaring one would put a member on our public
        // surface that the JDK's class does not have.
        super(undeclaredThrowable);
        this.undeclaredThrowable = undeclaredThrowable;
    }

    /**
     * Creates an exception wrapping the given throwable, with a detail message.
     *
     * @param undeclaredThrowable the throwable the handler threw
     * @param s the detail message
     */
    public UndeclaredThrowableException(Throwable undeclaredThrowable, String s) {
        super(s, undeclaredThrowable);
        this.undeclaredThrowable = undeclaredThrowable;
    }

    /**
     * Returns the wrapped throwable.
     *
     * @return the undeclared throwable
     */
    public Throwable getUndeclaredThrowable() {
        return this.undeclaredThrowable;
    }

}
