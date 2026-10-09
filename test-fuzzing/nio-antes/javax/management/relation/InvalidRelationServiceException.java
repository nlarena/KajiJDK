package javax.management.relation;

/**
 * The named relation service is not valid, or is not registered where it was said to be.
 */
public class InvalidRelationServiceException extends RelationException {

    private static final long serialVersionUID = 3400722103759507241L;

    /** Without detail. */
    public InvalidRelationServiceException() {
        super();
    }

    /** With a message. */
    public InvalidRelationServiceException(String message) {
        super(message);
    }
}
