package java.lang.management;

/**
 * KajiLibrary's java.lang.management.ClassLoadingMXBean -- cuantas clases se cargaron.
 *
 * <p>Tres contadores, y la relacion entre ellos es lo que hace util al MBean:
 * {@link #getLoadedClassCount} son las que estan cargadas <b>ahora</b>, y es igual a las cargadas en
 * total menos las descargadas.
 *
 * <p>Que las descargadas crezcan es normal --un cargador que se libera se lleva sus clases--; que las
 * cargadas actuales crezcan sin parar en un programa estable es la firma de una fuga de cargadores,
 * que es la fuga mas dificil de encontrar a mano.
 *
 * <p>{@link #setVerbose} activa el mismo rastreo que la opcion {@code -verbose:class}, y se puede
 * prender y apagar en caliente.
 */
public interface ClassLoadingMXBean extends PlatformManagedObject {

    /** Cuantas se cargaron desde que arranco la maquina virtual. */
    long getTotalLoadedClassCount();

    /** Cuantas estan cargadas ahora. Ver la nota de la clase. */
    int getLoadedClassCount();

    /** Cuantas se descargaron. */
    long getUnloadedClassCount();

    /** Si esta rastreando la carga de clases. */
    boolean isVerbose();

    /** Prende o apaga el rastreo. */
    void setVerbose(boolean value);
}
