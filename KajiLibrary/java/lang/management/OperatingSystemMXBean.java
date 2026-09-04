package java.lang.management;

/**
 * KajiLibrary's java.lang.management.OperatingSystemMXBean -- el sistema donde corre la maquina
 * virtual.
 *
 * <p>Cinco datos. Los cuatro primeros son las propiedades del sistema de siempre;
 * {@link #getSystemLoadAverage} es el unico que mide algo.
 *
 * <p>Ese devuelve la carga promedio del <b>ultimo minuto</b>, o un <b>negativo</b> si la plataforma no
 * la publica -- que es el caso de Windows. Es un numero relativo a la cantidad de procesadores: hay
 * que dividirlo por {@link #getAvailableProcessors} para saber si la maquina esta saturada.
 *
 * <p>{@link #getAvailableProcessors} puede <b>cambiar</b> entre llamadas: en un contenedor con cuota,
 * o en una maquina virtual que se redimensiona, no es constante.
 */
public interface OperatingSystemMXBean extends PlatformManagedObject {

    /** El nombre del sistema operativo. */
    String getName();

    /** La arquitectura. */
    String getArch();

    /** La version. */
    String getVersion();

    /** Cuantos procesadores ve la maquina virtual. Ver la nota de la clase: puede cambiar. */
    int getAvailableProcessors();

    /** La carga del ultimo minuto, o un negativo si no se publica. Ver la nota de la clase. */
    double getSystemLoadAverage();
}
