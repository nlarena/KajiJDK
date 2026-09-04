package javax.management.relation;

import javax.management.JMException;

/**
 * La raiz de los errores del servicio de relaciones.
 *
 * <p>Extiende {@link JMException} y no {@link RuntimeException}, y eso dice algo del diseno: en un
 * servicio de relaciones casi todo lo que puede fallar es <strong>estado</strong>, no programacion.
 * Que un rol no exista, que una relacion se haya borrado, que un MBean se haya desregistrado — todo
 * eso cambia mientras el sistema corre, asi que el compilador obliga a preverlo.
 */
public class RelationException extends JMException {

    private static final long serialVersionUID = 5434016005679159613L;

    /** Sin detalle. */
    public RelationException() {
        super();
    }

    /** Con un mensaje. */
    public RelationException(String message) {
        super(message);
    }
}
