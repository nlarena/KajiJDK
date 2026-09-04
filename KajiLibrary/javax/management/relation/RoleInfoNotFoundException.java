package javax.management.relation;

/**
 * El tipo de relacion no declara ningun rol con ese nombre.
 */
public class RoleInfoNotFoundException extends RelationException {

    private static final long serialVersionUID = 4394752332832935831L;

    /** Sin detalle. */
    public RoleInfoNotFoundException() {
        super();
    }

    /** Con un mensaje. */
    public RoleInfoNotFoundException(String message) {
        super(message);
    }
}
