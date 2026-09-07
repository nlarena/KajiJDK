package com.sun.jdi.request;

import com.sun.jdi.Locatable;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

/**
 * Pedir aviso al llegar a un punto del codigo.
 *
 * <p>La ubicacion se fija al crearlo y no se puede cambiar: para moverlo hay que borrarlo y crear
 * otro.
 *
 * @since 1.3
 */
public interface BreakpointRequest extends EventRequest,Locatable {

    /**
     * El location.
     *
     * @return el resultado
     */
    Location location();

    /**
     * Filtra por thread; solo con el pedido deshabilitado.
     *
     * @param thread el ThreadReference
     */
    void addThreadFilter(ThreadReference thread);

    /**
     * Filtra por instance; solo con el pedido deshabilitado.
     *
     * @param object el ObjectReference
     */
    void addInstanceFilter(ObjectReference object);
}
