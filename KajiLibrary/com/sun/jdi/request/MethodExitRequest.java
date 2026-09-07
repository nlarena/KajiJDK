package com.sun.jdi.request;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

/**
 * Pedir aviso al salir de un metodo.
 *
 * @since 1.3
 */
public interface MethodExitRequest extends EventRequest {

    /**
     * Filtra por thread; solo con el pedido deshabilitado.
     *
     * @param thread el ThreadReference
     */
    void addThreadFilter(ThreadReference thread);

    /**
     * Filtra por class; solo con el pedido deshabilitado.
     *
     * @param type el ReferenceType
     */
    void addClassFilter(ReferenceType type);

    /**
     * Filtra por class; solo con el pedido deshabilitado.
     *
     * @param name el String
     */
    void addClassFilter(String name);

    /**
     * Filtra por class exclusion; solo con el pedido deshabilitado.
     *
     * @param name el String
     */
    void addClassExclusionFilter(String name);

    /**
     * Filtra por instance; solo con el pedido deshabilitado.
     *
     * @param object el ObjectReference
     */
    void addInstanceFilter(ObjectReference object);
}
