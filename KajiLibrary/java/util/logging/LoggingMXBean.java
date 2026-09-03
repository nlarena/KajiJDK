package java.util.logging;

/**
 * KajiLibrary's java.util.logging.LoggingMXBean -- mirar y mover los niveles desde afuera.
 *
 * <p>Es la vista de administracion del arbol de loggers: que loggers hay, en que nivel esta cada
 * uno, quien es el padre de quien, y --la unica que escribe-- cambiarle el nivel a uno. Sirve para
 * subir el detalle de la traza de un servicio que ya esta corriendo sin reiniciarlo, que es cuando
 * mas falta hace y cuando menos se puede tocar el archivo de configuracion.
 *
 * <p><strong>Por que esta clase si se puede traer, aunque nombre a JMX.</strong> Su javadoc en el
 * JDK habla de `ManagementFactory` y de `PlatformLoggingMXBean`, y de ahi sale la idea de que
 * depende de `java.lang.management` --que en este arbol no existe--. Pero eso es **como se publica**,
 * no **que declara**: la interfaz son cuatro metodos sobre `String` y `List<String>` y ni uno solo
 * menciona un tipo de `java.lang.management`. Registrarla en un servidor MBean es otra cosa, y esa
 * otra cosa es la que falta; el contrato de estos cuatro metodos se cumple entero contra el
 * {@link LogManager} que ya esta aca. No hay nada que simular, asi que se trae.
 *
 * <p><strong>Los tres valores de retorno que hay que no confundir</strong>, porque son tres estados
 * distintos y dos de ellos se parecen:
 *
 * <ul>
 * <li><b>`null`</b> -- **no existe** un logger con ese nombre. Es la respuesta a una pregunta mal
 *     hecha, y por eso se distingue de las otras dos.
 * <li><b>`""`</b> -- el logger existe y **no tiene nivel propio**: hereda el del padre. Vacio y no
 *     `null` justamente para poder reservar `null` al caso de arriba.
 * <li><b>el nombre del nivel</b> -- el logger tiene nivel propio.
 * </ul>
 *
 * <p>Lo mismo en {@link #getParentLoggerName}: `""` es la **raiz** --existe y no tiene padre-- y
 * `null` es "no hay tal logger". Un solo valor para los dos casos haria imposible distinguir un
 * nombre mal escrito de la raiz, que es el error que uno comete al escribir la herramienta que
 * consume esto.
 *
 * <p>Esta deprecada desde 9 --lo que la reemplaza es `java.lang.management.PlatformLoggingMXBean`,
 * que vive del otro lado de la frontera--, pero **no** marcada para remocion: el JDK 25 la anota
 * `forRemoval=false`, y por eso la anotacion de aca dice lo mismo. Poner `forRemoval=true` seria
 * avisar de una remocion que la referencia no anuncia, y un aviso de mas es tan mentira como uno de
 * menos.
 */
@Deprecated(since = "9")
public interface LoggingMXBean {

    /**
     * Los nombres de todos los loggers registrados.
     *
     * <p>Una **foto**, no una vista viva: el que la recorre no se entera de los loggers que se creen
     * mientras la recorre, y eso es lo que corresponde -- crear un logger es algo que hace cualquier
     * clase al cargarse, y una lista que cambiara sola bajo el iterador convertiria un listado en una
     * carrera.
     */
    java.util.List<String> getLoggerNames();

    /**
     * El nombre del nivel propio de ese logger.
     *
     * @return el nombre del nivel, `""` si el logger hereda el nivel, o `null` si no existe
     */
    String getLoggerLevel(String loggerName);

    /**
     * Le pone el nivel a un logger que ya existe.
     *
     * <p>`levelName` en `null` **no es un error**: es la forma de sacarle el nivel propio y volver a
     * hacerlo heredar del padre. Es la operacion inversa de ponerselo, y sin ella se podria bajar el
     * detalle de un servicio en caliente pero no devolverlo a como estaba.
     *
     * <p>No crea el logger si no existe: mover el nivel de algo que no esta es no hacer nada, y
     * crearlo aca dejaria un logger sin dueno que nadie escribe.
     *
     * @throws IllegalArgumentException si no hay un logger con ese nombre, o si `levelName` no es un
     *         nivel conocido
     */
    void setLoggerLevel(String loggerName, String levelName);

    /**
     * El nombre del padre en el arbol.
     *
     * @return el nombre del padre, `""` si es la raiz --que no tiene--, o `null` si no existe
     */
    String getParentLoggerName(String loggerName);
}
