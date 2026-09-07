package com.sun.jdi.request;

import com.sun.jdi.ReferenceType;

/**
 * Pedir aviso cuando una clase se prepare.
 *
 * @since 1.3
 */
public interface ClassPrepareRequest extends EventRequest {

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
     * Filtra por source name; solo con el pedido deshabilitado.
     *
     * @param name el String
     */
    void addSourceNameFilter(String name);
}
