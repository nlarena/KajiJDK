package com.sun.jdi;

/**
 * InvalidCodeIndexException de la maquina depurada.
 *
 * @since 1.3
 */
public class InvalidCodeIndexException extends RuntimeException {

    /** Sin detalle. */
    public InvalidCodeIndexException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public InvalidCodeIndexException(String s) {
        super(s);
    }
}
