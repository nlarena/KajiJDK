package com.sun.jdi.event;

import com.sun.jdi.Mirror;
import java.util.Set;

/**
 * Los eventos que ocurrieron <strong>en el mismo punto</strong>, entregados juntos.
 *
 * <h2>Por que vienen en conjunto y no de a uno</h2>
 *
 * <p>Porque en un mismo instante puede haber pasado mas de una cosa: un punto de interrupcion y el
 * final de un paso, en la misma linea. Entregarlos por separado obligaria al depurador a adivinar
 * si el segundo es del mismo momento que el primero.
 *
 * <h2>El conjunto decide la suspension</h2>
 *
 * <p>Y esto es lo que hay que entender antes de escribir un depurador: cuando llega un conjunto,
 * los hilos que sus pedidos indicaron ya estan <strong>suspendidos</strong>. Siguen suspendidos
 * hasta que alguien llame a {@link #resume}.
 *
 * <p>Olvidarse de reanudar deja el programa depurado congelado sin ningun error a la vista. Es el
 * error mas comun con esta API.
 *
 * @since 1.3
 */
public interface EventSet extends Mirror, Set<Event> {

    /**
     * El suspend policy.
     *
     * @return el resultado
     */
    int suspendPolicy();

    /**
     * El event iterator.
     *
     * @return el resultado
     */
    EventIterator eventIterator();

    /**
     * El resume.
     */
    void resume();
}
