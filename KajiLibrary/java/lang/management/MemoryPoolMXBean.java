package java.lang.management;

/**
 * KajiLibrary's java.lang.management.MemoryPoolMXBean -- un area de memoria de la maquina virtual.
 *
 * <p>Cada area --la generacion joven, la vieja, el area de metodos, la cache de codigo-- tiene uno.
 * Es donde estan los numeros finos que {@link MemoryMXBean} resume en dos.
 *
 * <h2>Los dos umbrales</h2>
 *
 * <p>Es la parte que da valor a este MBean y la que se usa mal. Hay dos, y no miden lo mismo:
 *
 * <ul>
 *   <li>el de <b>uso</b> ({@link #setUsageThreshold}) se cruza cuando el area supera ese tamano en
 *       cualquier momento. En un programa sano se cruza todo el tiempo, porque la memoria sube entre
 *       recolecciones;
 *   <li>el de <b>uso tras recolectar</b> ({@link #setCollectionUsageThreshold}) se cruza solo si
 *       despues de que el recolector paso <b>sigue</b> habiendo mas de ese tamano ocupado.
 * </ul>
 *
 * <p>Para detectar una fuga sirve el segundo. Poner 0 apaga el umbral.
 *
 * <p>No todas las areas soportan los dos; hay que preguntar con
 * {@link #isUsageThresholdSupported} y {@link #isCollectionUsageThresholdSupported} antes, porque
 * usarlos sin soporte lanza {@link UnsupportedOperationException}.
 *
 * <h2>{@link #getPeakUsage} y {@link #resetPeakUsage}</h2>
 *
 * <p>El pico es desde que arranco la maquina virtual o desde el ultimo reinicio del contador.
 * Reiniciarlo antes de una operacion cara y leerlo despues es la forma de medir cuanta memoria pide
 * esa operacion, sin que la contaminen picos anteriores.
 */
public interface MemoryPoolMXBean extends PlatformManagedObject {

    /** Su nombre. */
    String getName();

    /** Monton o no monton. */
    MemoryType getType();

    /** Como esta ahora, o null si no se puede saber. */
    MemoryUsage getUsage();

    /** El maximo alcanzado. Ver la nota de la clase. */
    MemoryUsage getPeakUsage();

    /** Vuelve el pico al uso actual. */
    void resetPeakUsage();

    /** Si sigue vigente; puede dejar de estarlo. */
    boolean isValid();

    /** Que administradores la manejan. */
    String[] getMemoryManagerNames();

    /**
     * El umbral de uso, en bytes; 0 si esta apagado.
     *
     * @throws UnsupportedOperationException si esta area no lo soporta
     */
    long getUsageThreshold();

    /**
     * Lo fija; 0 lo apaga.
     *
     * @throws IllegalArgumentException si es negativo o mayor que el maximo
     * @throws UnsupportedOperationException si esta area no lo soporta
     */
    void setUsageThreshold(long threshold);

    /**
     * Si se cruzo.
     *
     * @throws UnsupportedOperationException si esta area no lo soporta
     */
    boolean isUsageThresholdExceeded();

    /**
     * Cuantas veces se cruzo.
     *
     * @throws UnsupportedOperationException si esta area no lo soporta
     */
    long getUsageThresholdCount();

    /** Si esta area soporta el umbral de uso. */
    boolean isUsageThresholdSupported();

    /**
     * El umbral de uso tras recolectar. Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException si esta area no lo soporta
     */
    long getCollectionUsageThreshold();

    /**
     * Lo fija; 0 lo apaga.
     *
     * @throws IllegalArgumentException si es negativo o mayor que el maximo
     * @throws UnsupportedOperationException si esta area no lo soporta
     */
    void setCollectionUsageThreshold(long threshold);

    /**
     * Si se cruzo.
     *
     * @throws UnsupportedOperationException si esta area no lo soporta
     */
    boolean isCollectionUsageThresholdExceeded();

    /**
     * Cuantas veces se cruzo.
     *
     * @throws UnsupportedOperationException si esta area no lo soporta
     */
    long getCollectionUsageThresholdCount();

    /** Como quedo despues de la ultima recoleccion, o null si nunca hubo una. */
    MemoryUsage getCollectionUsage();

    /** Si esta area soporta el umbral de uso tras recolectar. */
    boolean isCollectionUsageThresholdSupported();
}
