package com.sun.jdi.event;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;

/**
 * Se toco un campo que estaba vigilado.
 *
 * <p>{@link #valueCurrent} es el valor que tenia; en la variante de modificacion hay ademas el que
 * va a quedar.
 *
 * @since 1.3
 */
public interface WatchpointEvent extends LocatableEvent {

    /**
     * El field.
     *
     * @return el resultado
     */
    Field field();

    /**
     * El object.
     *
     * @return el resultado
     */
    ObjectReference object();

    /**
     * El value current.
     *
     * @return el resultado
     */
    Value valueCurrent();
}
