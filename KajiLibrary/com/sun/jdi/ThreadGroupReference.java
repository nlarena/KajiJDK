package com.sun.jdi;

import java.util.List;

/**
 * Un grupo de hilos de la maquina depurada.
 *
 * @since 1.3
 */
public interface ThreadGroupReference extends ObjectReference {

    /**
     * El nombre.
     *
     * @return el resultado
     */
    String name();

    /**
     * El parent.
     *
     * @return el resultado
     */
    ThreadGroupReference parent();

    /**
     * El suspend.
     */
    void suspend();

    /**
     * El resume.
     */
    void resume();

    /**
     * El threads.
     *
     * @return el resultado
     */
    List<ThreadReference> threads();

    /**
     * El thread groups.
     *
     * @return el resultado
     */
    List<ThreadGroupReference> threadGroups();
}
