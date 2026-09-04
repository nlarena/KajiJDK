package javax.management;

/**
 * Lo implementa el MBean que quiere enterarse de su propio registro.
 *
 * <p>Da dos poderes que no se ven a primera vista:
 *
 * <ul>
 *   <li>{@link #preRegister} <b>devuelve</b> un {@link ObjectName}. Un MBean puede elegir su propio
 *       nombre, y de hecho se puede registrar con `null` y dejar que el se nombre;
 *   <li>{@link #preRegister} puede tirar, y eso <b>cancela</b> el registro. Es la unica forma que
 *       tiene un MBean de negarse a existir en un agente que no le sirve.
 * </ul>
 *
 * <p>{@link #postRegister} recibe un `Boolean` --no un `boolean`-- porque tambien se llama cuando el
 * registro fracaso.
 */
public interface MBeanRegistration {

    /**
     * Antes de registrar. Devuelve el nombre definitivo.
     *
     * @throws Exception cancela el registro
     */
    ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception;

    /** Despues de intentar registrar; `registrationDone` dice si salio. */
    void postRegister(Boolean registrationDone);

    /**
     * Antes de dar de baja.
     *
     * @throws Exception cancela la baja
     */
    void preDeregister() throws Exception;

    /** Despues de la baja. */
    void postDeregister();
}
