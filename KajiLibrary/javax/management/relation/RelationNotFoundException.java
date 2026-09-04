package javax.management.relation;

/**
 * No hay ninguna relacion con ese identificador.
 */
public class RelationNotFoundException extends RelationException {

    private static final long serialVersionUID = -3793951411158559116L;

    /** Sin detalle. */
    public RelationNotFoundException() {
        super();
    }

    /** Con un mensaje. */
    public RelationNotFoundException(String message) {
        super(message);
    }
}
