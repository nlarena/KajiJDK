package com.sun.jdi;

/**
 * Un modulo de la maquina depurada.
 *
 * @since 1.3
 */
public interface ModuleReference extends ObjectReference {

    /**
     * El nombre.
     *
     * @return el resultado
     */
    String name();

    /**
     * El class loader.
     *
     * @return el resultado
     */
    ClassLoaderReference classLoader();
}
