package java.lang.management;

import java.util.List;

/**
 * KajiLibrary's java.lang.management.PlatformLoggingMXBean -- cambiar niveles de registro desde
 * afuera.
 *
 * <p>Existe para poder subir el detalle de un registro en un programa que ya esta corriendo, sin
 * reiniciarlo ni tocar archivos. Es lo que hace una consola de gestion cuando ofrece un selector de
 * nivel.
 *
 * <p>Todo se maneja con <b>cadenas</b> y no con los tipos de {@code java.util.logging}, a proposito:
 * asi una consola remota puede usar este MBean sin tener esas clases.
 *
 * <p>Hay dos ausencias que significan cosas distintas, y confundirlas es el error tipico:
 *
 * <ul>
 *   <li>{@link #getLoggerLevel} devuelve la <b>cadena vacia</b> si el registro no tiene nivel propio y
 *       lo hereda de su padre;
 *   <li>devuelve <b>null</b> si no existe registro con ese nombre.
 * </ul>
 *
 * <p>Y {@link #setLoggerLevel} con null vuelve a heredar del padre, que no es lo mismo que apagarlo.
 */
public interface PlatformLoggingMXBean extends PlatformManagedObject {

    /** Los nombres de los registros que existen ahora. */
    List<String> getLoggerNames();

    /** El nivel de ese registro; vacio si lo hereda, null si no existe. Ver la nota de la clase. */
    String getLoggerLevel(String loggerName);

    /**
     * Le fija el nivel; null lo hace heredar del padre.
     *
     * @throws IllegalArgumentException si el nivel no es uno conocido
     */
    void setLoggerLevel(String loggerName, String levelName);

    /** El nombre del padre, o la cadena vacia si es la raiz; null si no existe. */
    String getParentLoggerName(String loggerName);
}
