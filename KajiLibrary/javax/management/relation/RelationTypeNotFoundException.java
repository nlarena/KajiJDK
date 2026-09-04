package javax.management.relation;

/**
 * No hay ningun tipo de relacion con ese nombre.
 */
public class RelationTypeNotFoundException extends RelationException {

    private static final long serialVersionUID = 1274155316303520952L;

    /** Sin detalle. */
    public RelationTypeNotFoundException() {
        super();
    }

    /** Con un mensaje. */
    public RelationTypeNotFoundException(String message) {
        super(message);
    }
}
