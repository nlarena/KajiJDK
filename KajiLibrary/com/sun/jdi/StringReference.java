package com.sun.jdi;

/**
 * Una cadena de la maquina depurada.
 *
 * @since 1.3
 */
public interface StringReference extends ObjectReference {

    /**
     * El valor.
     *
     * @return el resultado
     */
    String value();
}
