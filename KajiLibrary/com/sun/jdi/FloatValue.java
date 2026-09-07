package com.sun.jdi;

/**
 * FloatValue de la maquina depurada.
 *
 * @since 1.3
 */
public interface FloatValue extends PrimitiveValue, Comparable<FloatValue> {

    /**
     * El valor.
     *
     * @return el resultado
     */
    float value();

    /**
     * Dos reflejos son iguales si nombran a lo mismo en la misma VM.
     *
     * @param obj el Object
     * @return el resultado
     */
    boolean equals(Object obj);

    /**
     * Coherente con {@link #equals}.
     *
     * @return el resultado
     */
    int hashCode();
}
