package com.sun.jdi;

import java.util.List;

/**
 * Un cargador de clases de la maquina depurada.
 *
 * @since 1.3
 */
public interface ClassLoaderReference extends ObjectReference {

    /**
     * El defined classes.
     *
     * @return el resultado
     */
    List<ReferenceType> definedClasses();

    /**
     * El visible classes.
     *
     * @return el resultado
     */
    List<ReferenceType> visibleClasses();
}
