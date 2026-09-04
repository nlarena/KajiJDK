package java.lang.management;

/**
 * KajiLibrary's java.lang.management.BufferPoolMXBean -- los buferes de {@code java.nio}.
 *
 * <p>Hay dos: {@code "direct"} y {@code "mapped"}. Son memoria que <b>no esta en el monton</b>, asi
 * que no aparece en {@link MemoryMXBean#getHeapMemoryUsage} y el recolector no la ve directamente.
 *
 * <p>Eso los hace la causa de una clase entera de problemas: un programa que agota la memoria de la
 * maquina mientras el monton se ve tranquilo casi siempre esta perdiendo buferes directos. Se liberan
 * cuando el objeto Java que los referencia se recolecta, y eso puede tardar arbitrariamente.
 *
 * <p>{@link #getTotalCapacity} es lo que los buferes dicen tener; {@link #getMemoryUsed} es lo que el
 * sistema operativo reservo de verdad, y puede ser -1 si no se sabe. Los dos juntos dicen si hay
 * fragmentacion.
 */
public interface BufferPoolMXBean extends PlatformManagedObject {

    /** {@code "direct"} o {@code "mapped"}. */
    String getName();

    /** Cuantos buferes hay en el conjunto. */
    long getCount();

    /** Cuanto dicen ocupar, en bytes. */
    long getTotalCapacity();

    /** Cuanto ocupan de verdad, o -1. Ver la nota de la clase. */
    long getMemoryUsed();
}
