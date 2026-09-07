package com.sun.jdi;

/**
 * InvalidModuleException de la maquina depurada.
 *
 * @since 1.3
 */
public class InvalidModuleException extends RuntimeException {

    /** Sin detalle. */
    public InvalidModuleException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public InvalidModuleException(String s) {
        super(s);
    }
}
