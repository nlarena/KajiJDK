package java.lang.reflect;

/**
 * Wraps an exception thrown by the method or constructor a reflective call invoked.
 *
 * <p>The wrapping is the point. A reflective invocation can fail in two entirely different ways —
 * the call itself was invalid (wrong argument count, inaccessible member), or the call succeeded and
 * the TARGET threw. Without a wrapper those two are indistinguishable at the call site, and a caller
 * that meant to handle the target's failure would silently swallow its own bug.
 *
 * <p>So the target's exception arrives boxed, reachable through {@link #getTargetException()}, and
 * everything else the reflective call can throw arrives unboxed.
 */
// Under ReflectiveOperationException, which is where the JDK groups everything that can fail while
// reflecting -- that way one `catch` covers this, ClassNotFoundException and
// NoSuchMethodException.
public class InvocationTargetException extends ReflectiveOperationException {

    private final Throwable target;

    /**
     * Creates an exception wrapping the given target.
     *
     * @param target the exception the invoked member threw
     */
    /**
     * With no target.
     *
     * <p>It is `protected` on purpose: nobody should be able to build this exception **without**
     * the one that caused it, because then it wraps nothing and loses its only reason for existing.
     * The JDK leaves it for the subclasses, which may have another way of supplying the target.
     */
    protected InvocationTargetException() {
        super((Throwable) null);
        this.target = null;
    }

    public InvocationTargetException(Throwable target) {
        super();
        this.target = target;
    }

    /**
     * Creates an exception wrapping the given target, with a detail message.
     *
     * @param target the exception the invoked member threw
     * @param s the detail message
     */
    public InvocationTargetException(Throwable target, String s) {
        super(s);
        this.target = target;
    }

    /**
     * Returns the exception the invoked member threw.
     *
     * @return the target exception
     */
    public Throwable getTargetException() {
        return this.target;
    }

    /**
     * Returns the exception the invoked member threw.
     *
     * @return the target exception, as the cause
     */
    public Throwable getCause() {
        return this.target;
    }
}
