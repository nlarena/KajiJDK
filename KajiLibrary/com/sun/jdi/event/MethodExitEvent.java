package com.sun.jdi.event;

import com.sun.jdi.Method;
import com.sun.jdi.Value;

/**
 * Se salio de un metodo.
 *
 * <p>{@link #returnValue} da lo que devolvio, que es lo unico que permite ver el resultado de una
 * llamada sin modificar el programa.
 *
 * @since 1.3
 */
public interface MethodExitEvent extends LocatableEvent {

    /**
     * El method.
     *
     * @return el resultado
     */
    Method method();

    /**
     * El return value.
     *
     * @return el resultado
     */
    Value returnValue();
}
