package com.sun.jdi.event;

import com.sun.jdi.Mirror;
import com.sun.jdi.request.EventRequest;

/**
 * Algo que paso en la maquina depurada y que alguien pidio que le avisaran.
 *
 * <p>Un evento no llega solo: llega dentro de un {@link EventSet}. Y no llega porque si, sino
 * porque hay un {@code EventRequest} que lo pidio -- {@link #request} devuelve cual.
 *
 * <p>Esa relacion es el modelo entero de JDI: se pide, y despues llega.
 *
 * @since 1.3
 */
public interface Event extends Mirror {

    /**
     * El request.
     *
     * @return el resultado
     */
    EventRequest request();
}
