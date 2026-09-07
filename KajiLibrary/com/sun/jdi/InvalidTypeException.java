package com.sun.jdi;

/**
 * InvalidTypeException de la maquina depurada.
 *
 * @since 1.3
 */
public class InvalidTypeException extends Exception {

    /** Sin detalle. */
    public InvalidTypeException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public InvalidTypeException(String s) {
        super(s);
    }
}
