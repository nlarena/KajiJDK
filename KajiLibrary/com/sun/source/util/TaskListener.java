package com.sun.source.util;

/**
 * Se entera de cada fase de la compilacion.
 *
 * <p>Los dos metodos tienen cuerpo vacio a proposito: casi nadie necesita los dos, y obligar a
 * escribir uno vacio no aporta nada. Es el mismo criterio que el de un adaptador de eventos, resuelto
 * con {@code default} en vez de con una clase extra.
 */
public interface TaskListener {

    /** Empieza una fase. */
    default void started(TaskEvent e) {
    }

    /** Termina una fase. */
    default void finished(TaskEvent e) {
    }
}
