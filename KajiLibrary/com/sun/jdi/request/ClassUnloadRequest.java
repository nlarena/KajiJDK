package com.sun.jdi.request;

/**
 * Pedir aviso cuando una clase se descargue.
 *
 * @since 1.3
 */
public interface ClassUnloadRequest extends EventRequest {

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
}
