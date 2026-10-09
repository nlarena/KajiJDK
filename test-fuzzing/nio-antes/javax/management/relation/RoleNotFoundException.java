package javax.management.relation;

/**
 * The relation has no role with that name, or it has one and it cannot be read or written.
 *
 * <p>The three cases share an exception because from outside they are the same: the role is not
 * available. Which of the three it was is said by the message.
 */
public class RoleNotFoundException extends RelationException {

    private static final long serialVersionUID = -1806664006012932146L;

    /** Without detail. */
    public RoleNotFoundException() {
        super();
    }

    /** With a message. */
    public RoleNotFoundException(String message) {
        super(message);
    }
}
