package com.sun.management;

/**
 * Los hilos, con la pregunta que la interfaz estandar no hace: cuanto memoria pidio cada uno.
 *
 * <h2>Por que la asignacion por hilo es un dato tan util</h2>
 *
 * <p>Porque la basura no la genera "el programa": la genera algun hilo, y casi siempre unos pocos.
 * Los acumulados de {@link java.lang.management.MemoryMXBean} dicen cuanto se recolecto y no dicen
 * quien lo produjo, que es lo unico que sirve para arreglarlo.
 *
 * <p>El contador es <strong>acumulado y monotono</strong>: cuenta todo lo que el hilo pidio desde
 * que arranco, sin restar lo que se recolecto. Por eso la forma de usarlo es tomar dos lecturas y
 * restarlas — el valor absoluto de una sola no dice nada.
 *
 * <h2>Por que hay que habilitarlo</h2>
 *
 * <p>Porque medir cuesta. {@link #isThreadAllocatedMemorySupported} dice si la VM puede,
 * {@link #isThreadAllocatedMemoryEnabled} si esta midiendo ahora, y
 * {@link #setThreadAllocatedMemoryEnabled} lo prende. Un hilo que corrio con la medicion apagada
 * no tiene el dato ni retroactivamente.
 *
 * <h2>Las versiones que toman un arreglo</h2>
 *
 * <p>{@link #getThreadCpuTime(long[])} y sus companeras existen por el costo de cruzar a la VM. Un
 * monitor que muestra doscientos hilos haria doscientas llamadas, cada una con su suspension; con
 * el arreglo hace una. El resultado esta en el mismo orden que la entrada, y una posicion vale
 * {@code -1} si ese hilo ya no existe.
 *
 * @since 1.5
 */
public interface ThreadMXBean extends java.lang.management.ThreadMXBean {

    /**
     * El tiempo de CPU de varios hilos, en nanosegundos.
     *
     * @param ids los identificadores
     * @return los tiempos, en el mismo orden; {@code -1} donde el hilo no existe o no se midio
     */
    long[] getThreadCpuTime(long[] ids);

    /**
     * El tiempo de CPU en modo usuario de varios hilos, en nanosegundos.
     *
     * @param ids los identificadores
     * @return los tiempos, en el mismo orden; {@code -1} donde el hilo no existe o no se midio
     */
    long[] getThreadUserTime(long[] ids);

    /**
     * La memoria total pedida por todos los hilos, vivos y terminados, en bytes.
     *
     * <p>Es el unico contador que incluye a los hilos que ya murieron, y por eso es el que sirve
     * para el total del proceso: sumar los vivos deja afuera todo lo que asigno un hilo de vida
     * corta, que suele ser la mayoria.
     *
     * @return los bytes, o {@code -1} si no hay dato
     * @throws UnsupportedOperationException si la VM no puede medir la asignacion por hilo
     */
    default long getTotalThreadAllocatedBytes() {
        if (!isThreadAllocatedMemorySupported()) {
            throw new UnsupportedOperationException(
                    "Thread allocated memory measurement is not supported.");
        }
        return -1L;
    }

    /**
     * La memoria pedida por el hilo que llama, en bytes.
     *
     * @return los bytes, o {@code -1} si la medicion esta apagada
     * @throws UnsupportedOperationException si la VM no puede medir la asignacion por hilo
     */
    default long getCurrentThreadAllocatedBytes() {
        return getThreadAllocatedBytes(Thread.currentThread().threadId());
    }

    /**
     * La memoria pedida por un hilo, en bytes.
     *
     * @param id el identificador del hilo
     * @return los bytes, o {@code -1} si el hilo no existe o la medicion esta apagada
     * @throws IllegalArgumentException si el identificador no es positivo
     * @throws UnsupportedOperationException si la VM no puede medir la asignacion por hilo
     */
    long getThreadAllocatedBytes(long id);

    /**
     * La memoria pedida por varios hilos, en bytes.
     *
     * @param ids los identificadores
     * @return los bytes, en el mismo orden; {@code -1} donde el hilo no existe o no se midio
     * @throws IllegalArgumentException si algun identificador no es positivo
     * @throws UnsupportedOperationException si la VM no puede medir la asignacion por hilo
     */
    long[] getThreadAllocatedBytes(long[] ids);

    /**
     * Si esta VM puede medir la asignacion por hilo.
     *
     * @return si puede
     */
    boolean isThreadAllocatedMemorySupported();

    /**
     * Si la esta midiendo ahora.
     *
     * @return si esta prendida
     * @throws UnsupportedOperationException si la VM no puede medirla
     */
    boolean isThreadAllocatedMemoryEnabled();

    /**
     * Prende o apaga la medicion.
     *
     * @param enable si prenderla
     * @throws UnsupportedOperationException si la VM no puede medirla
     */
    void setThreadAllocatedMemoryEnabled(boolean enable);
}
