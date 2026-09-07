package com.sun.jdi;

/**
 * ShortValue de la maquina depurada.
 *
 * @since 1.3
 */
public interface ShortValue extends PrimitiveValue, Comparable<ShortValue> {

    /**
     * El valor.
     *
     * @return el resultado
     */
    short value();

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
