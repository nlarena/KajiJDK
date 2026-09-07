package com.sun.jdi.event;

import com.sun.jdi.ThreadReference;

/**
 * La maquina depurada arranco y esta lista.
 *
 * <p>Llega con el hilo principal suspendido antes de la primera instruccion, que es la unica
 * ventana para configurar cosas antes de que el programa haga nada.
 *
 * @since 1.3
 */
public interface VMStartEvent extends Event {

    /**
     * El thread.
     *
     * @return el resultado
     */
    ThreadReference thread();
}
