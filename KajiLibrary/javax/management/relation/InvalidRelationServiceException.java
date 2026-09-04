package javax.management.relation;

/**
 * El servicio de relaciones que se nombro no es valido, o no esta registrado donde se dijo.
 */
public class InvalidRelationServiceException extends RelationException {

    private static final long serialVersionUID = 3400722103759507241L;

    /** Sin detalle. */
    public InvalidRelationServiceException() {
        super();
    }

    /** Con un mensaje. */
    public InvalidRelationServiceException(String message) {
        super(message);
    }
}
