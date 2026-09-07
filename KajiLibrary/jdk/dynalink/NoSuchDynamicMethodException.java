package jdk.dynalink;

/**
 * No member satisfies the operation requested at the call site.
 *
 * <p>It is the shape a `NoSuchMethodError` takes when the method is looked up at run time:
 * unchecked, because the code that provokes it does not declare that it can fail this way.
 *
 * @since 9
 */
public class NoSuchDynamicMethodException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoSuchDynamicMethodException(final String message) {
        super(message);
    }
}
