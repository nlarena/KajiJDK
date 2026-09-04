package javax.management.relation;

/**
 * El tipo de relacion no sirve: ya existe uno con ese nombre, o los roles que declara son
 * inconsistentes entre si.
 */
public class InvalidRelationTypeException extends RelationException {

    private static final long serialVersionUID = 3007446608299169973L;

    /** Sin detalle. */
    public InvalidRelationTypeException() {
        super();
    }

    /** Con un mensaje. */
    public InvalidRelationTypeException(String message) {
        super(message);
    }
}
