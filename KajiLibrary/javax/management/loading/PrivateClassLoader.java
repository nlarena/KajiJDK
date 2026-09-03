package javax.management.loading;

/**
 * KajiLibrary's javax.management.loading.PrivateClassLoader -- un cargador que no se comparte.
 *
 * <p>Interfaz marcadora, sin metodos: un cargador de clases que la implementa y se registra como
 * MBean <b>no</b> entra en el {@link ClassLoaderRepository} del agente.
 *
 * <p>Es un mecanismo de aislamiento y no de seguridad. Lo que evita es que dos MBeans que traen
 * distintas versiones de la misma biblioteca se resuelvan entre si por accidente al buscar una clase
 * por nombre en el repositorio -- que es el problema que aparece justo cuando ya es tarde, porque el
 * primero que se registro le gana al otro y el sintoma es un
 * {@code NoSuchMethodError} en tiempo de ejecucion.
 */
public interface PrivateClassLoader {
}
