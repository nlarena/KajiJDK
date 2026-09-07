package com.sun.jdi;

/**
 * InconsistentDebugInfoException de la maquina depurada.
 *
 * @since 1.3
 */
public class InconsistentDebugInfoException extends RuntimeException {

    /** Sin detalle. */
    public InconsistentDebugInfoException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public InconsistentDebugInfoException(String s) {
        super(s);
    }
}
