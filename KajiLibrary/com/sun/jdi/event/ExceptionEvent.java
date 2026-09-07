package com.sun.jdi.event;

import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;

/**
 * Se lanzo una excepcion en la maquina depurada.
 *
 * <p>{@link #catchLocation} devuelve donde se va a atrapar, o {@code null} si no la atrapa nadie.
 * Esa distincion es la que separa "romper en cualquier excepcion" de "romper solo en las que van a
 * matar al programa", que es lo que casi siempre se quiere.
 *
 * @since 1.3
 */
public interface ExceptionEvent extends LocatableEvent {

    /**
     * El exception.
     *
     * @return el resultado
     */
    ObjectReference exception();

    /**
     * El catch location.
     *
     * @return el resultado
     */
    Location catchLocation();
}
