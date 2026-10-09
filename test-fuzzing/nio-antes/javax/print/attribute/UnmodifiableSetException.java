package javax.print.attribute;

// The one thrown by `AttributeSetUtilities.unmodifiableView`'s read-only views when they are asked
// to modify. It is a RuntimeException: it is not declared, it is documented.
//
// It exists instead of reusing `UnsupportedOperationException` because the package was written
// before that became the collections' convention.
public class UnmodifiableSetException extends RuntimeException {

    private static final long serialVersionUID = 2255250308571511731L;

    public UnmodifiableSetException() {
        super();
    }

    public UnmodifiableSetException(String message) {
        super(message);
    }
}
