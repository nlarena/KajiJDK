package javax.management;

/**
 * While evaluating a query, the attribute had a value of a type the expression cannot compare.
 *
 * <p>The value is stored already converted to {@code String} in the constructor and not as the
 * original object. That early conversion is deliberate in the JDK: the exception is serializable,
 * and keeping the user's arbitrary object would force deserializing it on the other side.
 *
 * <p>Careful with {@link #toString()}: it says {@code "BadAttributeValueException"}, without the
 * {@code Exp} the class name does have. It is a JDK oddity kept because there is code that compares
 * it.
 */
public class BadAttributeValueExpException extends Exception {

    private static final long serialVersionUID = -3105272988410493376L;

    /**
     * @serial the offending value, already as a string
     */
    private String val;

    /** @param val the value that could not be used; its {@code toString()} is kept */
    public BadAttributeValueExpException(Object val) {
        this.val = val == null ? null : val.toString();
    }

    /** See the class note: it says {@code BadAttributeValueException}, without {@code Exp}. */
    public String toString() {
        return "BadAttributeValueException: " + val;
    }
}
