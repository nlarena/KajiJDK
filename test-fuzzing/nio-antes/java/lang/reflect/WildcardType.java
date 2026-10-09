package java.lang.reflect;

/**
 * A wildcard type argument: {@code ?}, {@code ? extends Number}, {@code ? super Integer}.
 *
 * <p>Both bound lists are arrays, which reads oddly for a language that allows only one bound on
 * either side. The shape is deliberately wider than the syntax so the model does not have to change
 * if the language ever admits intersection bounds — and it is why an unbounded {@code ?} reports an
 * upper bound of {@code [Object]} rather than an empty array: every wildcard has an upper bound,
 * even when it was not written.
 */
public interface WildcardType extends Type {

    /**
     * Returns the upper bounds of this wildcard. An unbounded wildcard reports {@code Object}.
     *
     * @return the upper bounds
     */
    Type[] getUpperBounds();

    /**
     * Returns the lower bounds of this wildcard, or an empty array if it has none.
     *
     * @return the lower bounds
     */
    Type[] getLowerBounds();
}
