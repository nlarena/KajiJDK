package jakarta.persistence.criteria;

/**
 * The {@code Order} interface of the Jakarta Persistence API.
 *
 * <p>Reconstructed from the published binary: the members and their generic
 * signatures come from the class file. What each one <em>means</em> is defined by
 * the Jakarta Persistence specification, not here.
 */
public interface Order {

    /** @return as defined by the specification. */
    Order reverse();

    /** @return as defined by the specification. */
    boolean isAscending();

    /** @return as defined by the specification. */
    Nulls getNullPrecedence();

    /** @return as defined by the specification. */
    Expression<?> getExpression();
}
