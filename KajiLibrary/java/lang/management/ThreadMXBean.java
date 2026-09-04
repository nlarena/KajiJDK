package java.lang.management;

/**
 * KajiLibrary's java.lang.management.ThreadMXBean -- los hilos de esta maquina virtual.
 *
 * <p>La interfaz mas grande del paquete, y la unica que puede <b>detectar interbloqueos</b>. Eso
 * ultimo es lo que la hace valiosa: es informacion que un programa no puede calcular por si mismo.
 *
 * <h2>Los dos buscadores de interbloqueo</h2>
 *
 * <p>{@link #findMonitorDeadlockedThreads} mira solo los monitores de {@code synchronized};
 * {@link #findDeadlockedThreads} mira ademas los candados de {@code java.util.concurrent}. Casi
 * siempre se quiere el segundo -- el primero es de una epoca en que el otro tipo de candado no
 * existia--.
 *
 * <p>Los dos devuelven <b>null</b> cuando no hay ninguno bloqueado, no un arreglo vacio. Es la trampa
 * mas comun de esta interfaz.
 *
 * <h2>Lo que hay que activar antes</h2>
 *
 * <p>Dos cosas estan apagadas por omision porque cuestan:
 *
 * <ul>
 *   <li>el seguimiento de <b>contencion</b> ({@link #setThreadContentionMonitoringEnabled}), sin el
 *       cual {@code getBlockedTime} y {@code getWaitedTime} devuelven -1;
 *   <li>el de <b>tiempo de procesador</b> ({@link #setThreadCpuTimeEnabled}), sin el cual
 *       {@code getThreadCpuTime} devuelve -1.
 * </ul>
 *
 * <p>Y no todas las maquinas virtuales los soportan; hay que preguntar con los {@code isXxxSupported}
 * antes, porque activarlos sin soporte lanza {@link UnsupportedOperationException}.
 *
 * <h2>Los identificadores se reusan</h2>
 *
 * <p>Un identificador de hilo vale mientras ese hilo vive. Cuando muere, la maquina virtual lo puede
 * volver a dar. Guardar identificadores y consultarlos mas tarde puede devolver informacion de otro
 * hilo, o null.
 */
public interface ThreadMXBean extends PlatformManagedObject {

    /** Cuantos hilos vivos hay, incluidos los demonio. */
    int getThreadCount();

    /** El maximo desde que arranco, o desde el ultimo {@link #resetPeakThreadCount}. */
    int getPeakThreadCount();

    /** Cuantos se crearon en total. */
    long getTotalStartedThreadCount();

    /** Cuantos de los vivos son demonio. */
    int getDaemonThreadCount();

    /** Los identificadores de los vivos. Ver la nota de la clase: se reusan. */
    long[] getAllThreadIds();

    /**
     * La informacion de ese hilo, sin pila.
     *
     * @return null si no hay hilo vivo con ese identificador
     * @throws IllegalArgumentException si el identificador no es positivo
     */
    ThreadInfo getThreadInfo(long id);

    /** Idem, de varios; la posicion que no exista queda en null. */
    ThreadInfo[] getThreadInfo(long[] ids);

    /**
     * Idem, con hasta esa cantidad de marcos de pila.
     *
     * @param maxDepth cuantos marcos como maximo; {@link Integer#MAX_VALUE} para todos
     * @throws IllegalArgumentException si la profundidad es negativa
     */
    ThreadInfo getThreadInfo(long id, int maxDepth);

    /** Idem, de varios. */
    ThreadInfo[] getThreadInfo(long[] ids, int maxDepth);

    /** Si esta maquina virtual sabe medir contencion. */
    boolean isThreadContentionMonitoringSupported();

    /**
     * Si esta activado.
     *
     * @throws UnsupportedOperationException si no lo soporta
     */
    boolean isThreadContentionMonitoringEnabled();

    /**
     * Lo activa o lo apaga. Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException si no lo soporta
     */
    void setThreadContentionMonitoringEnabled(boolean enable);

    /**
     * Nanosegundos de procesador del hilo actual, o -1 si esta apagado.
     *
     * @throws UnsupportedOperationException si no lo soporta
     */
    long getCurrentThreadCpuTime();

    /**
     * Idem, solo el tiempo en modo usuario.
     *
     * @throws UnsupportedOperationException si no lo soporta
     */
    long getCurrentThreadUserTime();

    /**
     * Nanosegundos de procesador de ese hilo, o -1.
     *
     * @throws UnsupportedOperationException si no lo soporta
     */
    long getThreadCpuTime(long id);

    /**
     * Idem, en modo usuario.
     *
     * @throws UnsupportedOperationException si no lo soporta
     */
    long getThreadUserTime(long id);

    /** Si sabe medir el tiempo de procesador de cualquier hilo. */
    boolean isThreadCpuTimeSupported();

    /** Si sabe medirlo al menos del hilo actual. */
    boolean isCurrentThreadCpuTimeSupported();

    /**
     * Si la medicion esta activada.
     *
     * @throws UnsupportedOperationException si no lo soporta
     */
    boolean isThreadCpuTimeEnabled();

    /**
     * La activa o la apaga.
     *
     * @throws UnsupportedOperationException si no lo soporta
     */
    void setThreadCpuTimeEnabled(boolean enable);

    /**
     * Los hilos bloqueados en un ciclo de monitores.
     *
     * @return null si no hay ninguno. Ver la nota de la clase.
     */
    long[] findMonitorDeadlockedThreads();

    /** Pone el pico en la cantidad actual. */
    void resetPeakThreadCount();

    /**
     * Los hilos bloqueados en un ciclo, contando tambien los candados de
     * {@code java.util.concurrent}.
     *
     * @return null si no hay ninguno
     * @throws UnsupportedOperationException si esta maquina virtual no sabe mirar esos candados
     */
    long[] findDeadlockedThreads();

    /** Si sabe informar que monitores tiene tomados un hilo. */
    boolean isObjectMonitorUsageSupported();

    /** Si sabe informar que candados de {@code java.util.concurrent} tiene tomados. */
    boolean isSynchronizerUsageSupported();

    /**
     * La informacion de esos hilos, con la pila entera y opcionalmente los candados.
     *
     * @param lockedMonitors si incluir los monitores tomados
     * @param lockedSynchronizers si incluir los candados de {@code java.util.concurrent}
     * @throws UnsupportedOperationException si se pide algo que esta maquina virtual no soporta
     */
    ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors, boolean lockedSynchronizers);

    /**
     * Idem, limitando la profundidad de pila.
     *
     * <p>Por omision ignora el limite y delega en la version sin el; una maquina virtual que sepa
     * cortar la pila redefine esto para no pagar por marcos que se van a descartar.
     */
    default ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
                                       boolean lockedSynchronizers, int maxDepth) {
        throw new UnsupportedOperationException();
    }

    /** La informacion de <b>todos</b> los hilos vivos. */
    ThreadInfo[] dumpAllThreads(boolean lockedMonitors, boolean lockedSynchronizers);

    /** Idem, limitando la profundidad. */
    default ThreadInfo[] dumpAllThreads(boolean lockedMonitors, boolean lockedSynchronizers,
                                        int maxDepth) {
        throw new UnsupportedOperationException();
    }
}
