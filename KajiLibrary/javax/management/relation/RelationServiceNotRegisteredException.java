package javax.management.relation;

/**
 * El servicio de relaciones existe pero no esta registrado en ningun servidor de MBeans.
 *
 * <p>Es una distincion util: el objeto se puede construir y configurar antes de registrarlo, pero
 * casi ninguna operacion sirve hasta que este — necesita el servidor para verificar que los MBeans
 * referenciados existan.
 */
public class RelationServiceNotRegisteredException extends RelationException {

    private static final long serialVersionUID = 8454744887157122910L;

    /** Sin detalle. */
    public RelationServiceNotRegisteredException() {
        super();
    }

    /** Con un mensaje. */
    public RelationServiceNotRegisteredException(String message) {
        super(message);
    }
}
