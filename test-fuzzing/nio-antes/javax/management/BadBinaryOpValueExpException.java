package javax.management;

/**
 * A binary operator was applied to an expression that does not admit it.
 *
 * <p>Unlike {@link BadAttributeValueExpException}, this one does keep the whole expression: a
 * {@link ValueExp} belongs to JMX itself and serializing it drags no user types along.
 */
public class BadBinaryOpValueExpException extends Exception {

    private static final long serialVersionUID = 5068475589449021227L;

    /**
     * @serial the offending expression
     */
    private ValueExp exp;

    /** @param exp the expression the operator could not be applied to */
    public BadBinaryOpValueExpException(ValueExp exp) {
        this.exp = exp;
    }

    /** The offending expression. */
    public ValueExp getExp() {
        return exp;
    }

    public String toString() {
        return "BadBinaryOpValueExpException: " + exp;
    }
}
