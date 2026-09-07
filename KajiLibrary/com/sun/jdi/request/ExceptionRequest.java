package com.sun.jdi.request;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

/**
 * Pedir aviso cuando se lance una excepcion.
 *
 * <p>Los dos booleanos del constructor deciden si interesan las atrapadas, las no atrapadas o las
 * dos. Filtrar por "no atrapadas" es lo que convierte a esto en una herramienta usable: un programa
 * normal lanza y atrapa excepciones todo el tiempo.
 *
 * @since 1.3
 */
public interface ExceptionRequest extends EventRequest {

    /**
     * El exception.
     *
     * @return el resultado
     */
    ReferenceType exception();

    /**
     * El notify caught.
     *
     * @return el resultado
     */
    boolean notifyCaught();

    /**
     * El notify uncaught.
     *
     * @return el resultado
     */
    boolean notifyUncaught();

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
