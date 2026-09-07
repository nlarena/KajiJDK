package com.sun.jdi.event;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

/**
 * Una clase termino de prepararse en la maquina depurada.
 *
 * <p>Es el momento en que se le pueden poner puntos de interrupcion: antes de prepararla no hay
 * codigo al que apuntar. Un depurador que quiere romper en una clase que todavia no se cargo pide
 * este evento y espera.
 *
 * @since 1.3
 */
public interface ClassPrepareEvent extends Event {

    /**
     * El thread.
     *
     * @return el resultado
     */
    ThreadReference thread();

    /**
     * El reference type.
     *
     * @return el resultado
     */
    ReferenceType referenceType();
}
