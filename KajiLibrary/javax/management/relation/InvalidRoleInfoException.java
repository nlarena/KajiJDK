package javax.management.relation;

/**
 * A role's description is contradictory.
 *
 * <p>The typical case is a minimum degree greater than the maximum, which makes the role impossible
 * to fulfil -- and that is why it is rejected when the type is declared and not when it is used.
 */
public class InvalidRoleInfoException extends RelationException {

    private static final long serialVersionUID = 7517834705158932074L;

    /** Without detail. */
    public InvalidRoleInfoException() {
        super();
    }

    /** With a message. */
    public InvalidRoleInfoException(String message) {
        super(message);
    }
}
