package com.sun.jdi.event;

import com.sun.jdi.Locatable;
import com.sun.jdi.ThreadReference;

/**
 * Un evento que ocurrio en un punto conocido del codigo.
 *
 * @since 1.3
 */
public interface LocatableEvent extends Event,Locatable {

    /**
     * El thread.
     *
     * @return el resultado
     */
    ThreadReference thread();
}
