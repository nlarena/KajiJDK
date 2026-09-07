package com.sun.jdi;

/**
 * IntegerValue de la maquina depurada.
 *
 * @since 1.3
 */
public interface IntegerValue extends PrimitiveValue, Comparable<IntegerValue> {

    /**
     * El valor.
     *
     * @return el resultado
     */
    int value();

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
