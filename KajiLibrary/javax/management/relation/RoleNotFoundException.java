package javax.management.relation;

/**
 * La relacion no tiene ningun rol con ese nombre, o lo tiene y no se lo puede leer o escribir.
 *
 * <p>Los tres casos comparten excepcion porque desde afuera son el mismo: el rol no esta
 * disponible. Cual de los tres fue lo dice el mensaje.
 */
public class RoleNotFoundException extends RelationException {

    private static final long serialVersionUID = -1806664006012932146L;

    /** Sin detalle. */
    public RoleNotFoundException() {
        super();
    }

    /** Con un mensaje. */
    public RoleNotFoundException(String message) {
        super(message);
    }
}
