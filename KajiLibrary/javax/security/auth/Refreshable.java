package javax.security.auth;

/**
 * KajiLibrary's javax.security.auth.Refreshable -- una credencial que vence y se puede renovar.
 *
 * <p>Los dos metodos son abstractos, a diferencia de {@link Destroyable}, que los tiene por defecto.
 * La razon es que aca no hay ningun default seguro: {@code isCurrent()} tendria que decir false --
 * "no se si sigue vigente" -- y entonces {@code refresh()} se llamaria siempre, o decir true y
 * mentir. Quien implementa esta interfaz es porque sabe cuando vence su credencial; el que no sabe
 * no la implementa.
 */
public interface Refreshable {

    /** Si la credencial sigue vigente. */
    boolean isCurrent();

    /**
     * Renueva la credencial.
     *
     * @throws RefreshFailedException si no se pudo -- la credencial vieja puede seguir sirviendo
     */
    void refresh() throws RefreshFailedException;
}
