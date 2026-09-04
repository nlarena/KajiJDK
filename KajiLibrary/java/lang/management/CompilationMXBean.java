package java.lang.management;

/**
 * KajiLibrary's java.lang.management.CompilationMXBean -- el compilador de tiempo de ejecucion.
 *
 * <p>Existe solo si la maquina virtual tiene uno: {@code ManagementFactory.getCompilationMXBean()}
 * devuelve <b>null</b> en una que solo interprete. Es de los pocos lugares de la API donde null es una
 * respuesta correcta y no un error.
 *
 * <p>{@link #getTotalCompilationTime} es tiempo acumulado de <b>todos</b> los hilos de compilacion, no
 * tiempo de reloj, asi que puede ser mayor que lo que lleva corriendo el programa. Y es aproximado:
 * la propia documentacion advierte que puede no ser monotono si la maquina virtual reajusta su
 * medicion.
 */
public interface CompilationMXBean extends PlatformManagedObject {

    /** El nombre del compilador. */
    String getName();

    /** Si sabe medir cuanto tarda compilando. */
    boolean isCompilationTimeMonitoringSupported();

    /**
     * Milisegundos acumulados compilando. Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException si no sabe medirlo
     */
    long getTotalCompilationTime();
}
