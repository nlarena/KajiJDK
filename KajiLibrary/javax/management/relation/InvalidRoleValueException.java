package javax.management.relation;

/**
 * A role's value does not meet what its description requires.
 *
 * <p>How many MBeans there are, which class they are, whether they are registered: any of the
 * {@link RoleInfo} conditions that is not met comes through here. The precise reason is in
 * {@link RoleStatus}.
 */
public class InvalidRoleValueException extends RelationException {

    private static final long serialVersionUID = -2066091747301983721L;

    /** Without detail. */
    public InvalidRoleValueException() {
        super();
    }

    /** With a message. */
    public InvalidRoleValueException(String message) {
        super(message);
    }
}
