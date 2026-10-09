package com.sun.jdi.event;

import com.sun.jdi.Locatable;
import com.sun.jdi.ThreadReference;

/**
 * An event that happened at a known point of the code.
 *
 * @since 1.3
 */
public interface LocatableEvent extends Event,Locatable {

    /**
     * The thread.
     *
     * @return the result
     */
    ThreadReference thread();
}
