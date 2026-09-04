package java.lang.management;

import java.util.List;
import java.util.Map;

/**
 * KajiLibrary's java.lang.management.RuntimeMXBean -- como se arranco esta maquina virtual.
 *
 * <p>La foto de arranque: version, rutas de clases, argumentos, propiedades del sistema, y desde
 * cuando esta corriendo.
 *
 * <h2>{@link #getInputArguments} no trae todo</h2>
 *
 * <p>Trae lo que se le paso a la maquina virtual, y explicitamente <b>no</b> los argumentos del
 * {@code main}. Tampoco lo que llego por {@code JAVA_TOOL_OPTIONS} o por un archivo de opciones,
 * segun la maquina virtual. Para reproducir una corrida no alcanza con esto.
 *
 * <h2>{@link #getName} no promete nada</h2>
 *
 * <p>La documentacion dice que puede ser cualquier cadena. En la practica los JDK devuelven
 * {@code pid@maquina}, y hay codigo por ahi que lo parsea para sacar el pid. Es fragil, y para eso
 * esta {@link #getPid}, que llego despues justamente por eso.
 *
 * <h2>{@link #getUptime} y {@link #getStartTime}</h2>
 *
 * <p>El primero es un contador de milisegundos desde el arranque y no depende del reloj de pared; el
 * segundo si. Para medir intervalos hay que usar el primero: el reloj del sistema puede saltar.
 */
public interface RuntimeMXBean extends PlatformManagedObject {

    /**
     * El identificador de proceso.
     *
     * <p>Por omision lo saca de {@code ProcessHandle.current()}, asi que hereda lo que esa clase
     * pueda o no hacer en esta plataforma.
     */
    default long getPid() {
        return ProcessHandle.current().pid();
    }

    /** El nombre de esta maquina virtual. Ver la nota de la clase: no promete formato. */
    String getName();

    /** El nombre de la implementacion. */
    String getVmName();

    /** Quien la hizo. */
    String getVmVendor();

    /** Su version. */
    String getVmVersion();

    /** El nombre de la especificacion que cumple. */
    String getSpecName();

    /** Quien la escribio. */
    String getSpecVendor();

    /** Que version de la especificacion. */
    String getSpecVersion();

    /** Que version de la especificacion de gestion cumple este MBean. */
    String getManagementSpecVersion();

    /** La ruta de clases. */
    String getClassPath();

    /** La ruta de bibliotecas nativas. */
    String getLibraryPath();

    /** Si esta maquina virtual soporta el concepto de ruta de arranque. */
    boolean isBootClassPathSupported();

    /**
     * La ruta de arranque.
     *
     * @throws UnsupportedOperationException si esta maquina virtual no la soporta
     */
    String getBootClassPath();

    /** Los argumentos de arranque. Ver la nota de la clase: no estan los del {@code main}. */
    List<String> getInputArguments();

    /** Milisegundos corriendo. Ver la nota de la clase. */
    long getUptime();

    /** Cuando arranco, en milisegundos desde el epoch. */
    long getStartTime();

    /** Las propiedades del sistema, como mapa de cadenas. */
    Map<String, String> getSystemProperties();
}
