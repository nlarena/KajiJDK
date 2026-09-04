package jdk.management;

import java.lang.management.PlatformManagedObject;

/**
 * La vista de administracion del planificador de hilos virtuales.
 *
 * <p>Un hilo virtual no tiene un hilo del sistema operativo propio: corre <em>montado</em> sobre uno
 * de un pool de hilos portadores, y se desmonta cuando se bloquea. Este MXBean expone las cuatro
 * cantidades que describen ese pool en un momento dado —cuantos portadores hay, cuantos hilos
 * virtuales estan montados, cuantos esperan en cola— y la unica perilla: el paralelismo.
 *
 * <p>Es una interfaz de <em>lectura mas una escritura</em>, y la asimetria es deliberada: el tamano
 * del pool y la cantidad de montados son consecuencias, no decisiones. Lo unico que se elige es
 * cuantos portadores puede usar el planificador a la vez.
 *
 * @since 24
 */
public interface VirtualThreadSchedulerMXBean extends PlatformManagedObject {

    /** Cuantos hilos portadores puede usar el planificador a la vez. */
    int getParallelism();

    /**
     * Cambia el paralelismo.
     *
     * @throws IllegalArgumentException si el valor no es positivo, o excede el maximo del
     *     planificador
     */
    void setParallelism(int size);

    /**
     * Cuantos hilos portadores existen ahora.
     *
     * <p>No tiene por que coincidir con {@link #getParallelism}: el pool crece bajo demanda y puede
     * quedar por encima del paralelismo mientras hay portadores bloqueados.
     */
    int getPoolSize();

    /** Cuantos hilos virtuales estan montados sobre un portador en este momento. */
    int getMountedVirtualThreadCount();

    /** Cuantos hilos virtuales estan encolados esperando un portador. */
    long getQueuedVirtualThreadCount();
}
