package javax.management.relation;

/**
 * There is no relation type with that name.
 */
public class RelationTypeNotFoundException extends RelationException {

    private static final long serialVersionUID = 1274155316303520952L;

    /** Without detail. */
    public RelationTypeNotFoundException() {
        super();
    }

    /** With a message. */
    public RelationTypeNotFoundException(String message) {
        super(message);
    }
}
