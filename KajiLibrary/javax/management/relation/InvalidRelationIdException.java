package javax.management.relation;

/**
 * The relation identifier is not valid: either it is already in use, or it does not exist when it
 * should.
 */
public class InvalidRelationIdException extends RelationException {

    private static final long serialVersionUID = -7115040321202754171L;

    /** Without detail. */
    public InvalidRelationIdException() {
        super();
    }

    /** With a message. */
    public InvalidRelationIdException(String message) {
        super(message);
    }
}
