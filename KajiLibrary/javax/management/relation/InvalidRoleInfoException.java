package javax.management.relation;

/**
 * La descripcion de un rol es contradictoria.
 *
 * <p>El caso tipico es un grado minimo mayor que el maximo, que hace imposible cumplir el rol —
 * y por eso se rechaza al declarar el tipo y no al usarlo.
 */
public class InvalidRoleInfoException extends RelationException {

    private static final long serialVersionUID = 7517834705158932074L;

    /** Sin detalle. */
    public InvalidRoleInfoException() {
        super();
    }

    /** Con un mensaje. */
    public InvalidRoleInfoException(String message) {
        super(message);
    }
}
