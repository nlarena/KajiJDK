package com.sun.jdi;

/**
 * LongValue de la maquina depurada.
 *
 * @since 1.3
 */
public interface LongValue extends PrimitiveValue, Comparable<LongValue> {

    /**
     * El valor.
     *
     * @return el resultado
     */
    long value();

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
