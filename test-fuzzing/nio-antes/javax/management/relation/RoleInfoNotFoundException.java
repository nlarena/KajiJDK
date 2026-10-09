package javax.management.relation;

/**
 * The relation type declares no role with that name.
 */
public class RoleInfoNotFoundException extends RelationException {

    private static final long serialVersionUID = 4394752332832935831L;

    /** Without detail. */
    public RoleInfoNotFoundException() {
        super();
    }

    /** With a message. */
    public RoleInfoNotFoundException(String message) {
        super(message);
    }
}
