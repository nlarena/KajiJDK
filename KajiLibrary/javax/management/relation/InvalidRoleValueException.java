package javax.management.relation;

/**
 * El valor de un rol no cumple lo que su descripcion exige.
 *
 * <p>Cuantos MBeans hay, de que clase son, si estan registrados: cualquiera de las condiciones de
 * {@link RoleInfo} que no se cumpla llega por aca. El motivo preciso esta en {@link RoleStatus}.
 */
public class InvalidRoleValueException extends RelationException {

    private static final long serialVersionUID = -2066091747301983721L;

    /** Sin detalle. */
    public InvalidRoleValueException() {
        super();
    }

    /** Con un mensaje. */
    public InvalidRoleValueException(String message) {
        super(message);
    }
}
