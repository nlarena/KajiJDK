package jdk.dynalink.linker;

/**
 * Un enlazador: dado un sitio de invocacion y los argumentos reales, produce el metodo a llamar.
 *
 * <p>Es la interfaz central de Dynalink y la unica que un lenguaje dinamico tiene que implementar
 * para integrarse. Todo lo demas del paquete existe para servirla.
 *
 * <h2>Por que devuelve una invocacion con guarda y no un metodo</h2>
 *
 * <p>Porque el enlace se hace una vez y se usa muchas. La respuesta no puede ser "para estos
 * argumentos, este metodo" —eso obligaria a volver a preguntar en cada llamada— sino "mientras se
 * cumpla esta condicion, este metodo". La condicion es la guarda de {@link GuardedInvocation}, y
 * es lo que convierte el enlace en cache.
 *
 * <h2>Que significa devolver null</h2>
 *
 * <p>Que este enlazador no sabe manejar ese sitio, no que el sitio sea invalido. Los enlazadores
 * se componen en cadena y el {@code null} es lo que pasa el turno al siguiente. Tirar una
 * excepcion, en cambio, corta la cadena.
 *
 * @since 9
 */
@FunctionalInterface
public interface GuardingDynamicLinker {

    /**
     * El metodo a llamar, con la condicion bajo la cual sigue valiendo.
     *
     * @param linkRequest el sitio y los argumentos reales de la primera invocacion
     * @param linkerServices lo que el enlazador puede pedirle al que lo hospeda
     * @return la invocacion con su guarda, o {@code null} si este enlazador no sabe manejarlo
     * @throws Exception si el enlace falla de verdad; corta la cadena de enlazadores
     */
    GuardedInvocation getGuardedInvocation(LinkRequest linkRequest, LinkerServices linkerServices)
            throws Exception;
}
