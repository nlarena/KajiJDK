package com.sun.jdi.request;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

/**
 * Pedir aviso cuando un hilo avance lo que se indique.
 *
 * <p>El tamano dice en que unidad --instruccion o linea-- y la profundidad dice si entrar en las
 * llamadas, saltearlas o salir de la actual. Son las tres cosas que en un depurador se llaman "step
 * into", "step over" y "step out".
 *
 * <p>Solo puede haber un pedido de paso por hilo: crear el segundo tira
 * {@link DuplicateRequestException}.
 *
 * @since 1.3
 */
public interface StepRequest extends EventRequest {

    /** Entrar en las llamadas. */
    int STEP_INTO = 1;

    /** Saltear las llamadas. */
    int STEP_OVER = 2;

    /** Salir del metodo actual. */
    int STEP_OUT = 3;

    /** Avanzar una instruccion de bytecode. */
    int STEP_MIN = -1;

    /** Avanzar una linea de fuente. */
    int STEP_LINE = -2;

    /**
     * El thread.
     *
     * @return el resultado
     */
    ThreadReference thread();

    /**
     * El size.
     *
     * @return el resultado
     */
    int size();

    /**
     * El depth.
     *
     * @return el resultado
     */
    int depth();

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
