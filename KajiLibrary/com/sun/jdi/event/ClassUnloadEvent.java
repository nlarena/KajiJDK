package com.sun.jdi.event;

/**
 * Una clase se descargo de la maquina depurada.
 *
 * @since 1.3
 */
public interface ClassUnloadEvent extends Event {

    /**
     * El class name.
     *
     * @return el resultado
     */
    String className();

    /**
     * El class signature.
     *
     * @return el resultado
     */
    String classSignature();
}
