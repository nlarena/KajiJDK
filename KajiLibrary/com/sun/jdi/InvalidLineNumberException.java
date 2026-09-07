package com.sun.jdi;

/**
 * InvalidLineNumberException de la maquina depurada.
 *
 * @since 1.3
 */
public class InvalidLineNumberException extends RuntimeException {

    /** Sin detalle. */
    public InvalidLineNumberException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public InvalidLineNumberException(String s) {
        super(s);
    }
}
