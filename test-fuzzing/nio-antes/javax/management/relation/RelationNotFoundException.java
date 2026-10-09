package javax.management.relation;

/**
 * There is no relation with that identifier.
 */
public class RelationNotFoundException extends RelationException {

    private static final long serialVersionUID = -3793951411158559116L;

    /** Without detail. */
    public RelationNotFoundException() {
        super();
    }

    /** With a message. */
    public RelationNotFoundException(String message) {
        super(message);
    }
}
