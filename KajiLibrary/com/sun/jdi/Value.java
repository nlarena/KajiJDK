package com.sun.jdi;

/**
 * Un valor de la maquina depurada.
 *
 * <p>Se parte en dos ramas que no se mezclan: {@link PrimitiveValue}, que lleva el dato adentro, y
 * {@link ObjectReference}, que lleva un identificador. Un {@code int} viaja; un objeto no.
 *
 * @since 1.3
 */
public interface Value extends Mirror {

    /**
     * El tipo.
     *
     * @return el resultado
     */
    Type type();
}
