package com.sun.jdi;

/**
 * DoubleValue de la maquina depurada.
 *
 * @since 1.3
 */
public interface DoubleValue extends PrimitiveValue, Comparable<DoubleValue> {

    /**
     * El valor.
     *
     * @return el resultado
     */
    double value();

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
