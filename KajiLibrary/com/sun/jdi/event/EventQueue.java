package com.sun.jdi.event;

import com.sun.jdi.Mirror;

/**
 * De donde se sacan los conjuntos de eventos, uno por vez.
 *
 * <p>{@link #remove()} bloquea hasta que haya algo. Un depurador tiene un hilo dedicado a este
 * bucle, porque mientras espera no puede hacer nada mas.
 *
 * <p>Cuando la conexion se corta, {@code remove} tira {@link VMDisconnectedException} despues de
 * entregar el {@link VMDisconnectEvent}: primero el aviso ordenado, despues el corte.
 *
 * @since 1.3
 */
public interface EventQueue extends Mirror {

    /**
     * El remove.
     *
     * @return el resultado
     * @throws InterruptedException si corresponde
     */
    EventSet remove()
            throws InterruptedException;

    /**
     * El remove.
     *
     * @param index el long
     * @return el resultado
     * @throws InterruptedException si corresponde
     */
    EventSet remove(long index)
            throws InterruptedException;
}
