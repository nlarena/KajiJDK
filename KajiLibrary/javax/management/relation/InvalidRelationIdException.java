package javax.management.relation;

/**
 * El identificador de relacion no sirve: o ya esta en uso, o no existe cuando deberia.
 */
public class InvalidRelationIdException extends RelationException {

    private static final long serialVersionUID = -7115040321202754171L;

    /** Sin detalle. */
    public InvalidRelationIdException() {
        super();
    }

    /** Con un mensaje. */
    public InvalidRelationIdException(String message) {
        super(message);
    }
}
