package javax.management.relation;

/**
 * The relation type is not valid: there is already one with that name, or the roles it declares
 * are inconsistent with each other.
 */
public class InvalidRelationTypeException extends RelationException {

    private static final long serialVersionUID = 3007446608299169973L;

    /** Without detail. */
    public InvalidRelationTypeException() {
        super();
    }

    /** With a message. */
    public InvalidRelationTypeException(String message) {
        super(message);
    }
}
