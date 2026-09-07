package com.sun.jdi;

/**
 * Lo que tiene una posicion en el codigo.
 *
 * @since 1.3
 */
public interface Locatable {

    /**
     * El location.
     *
     * @return el resultado
     */
    Location location();
}
