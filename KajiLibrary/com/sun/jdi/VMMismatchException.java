package com.sun.jdi;

/**
 * VMMismatchException de la maquina depurada.
 *
 * @since 1.3
 */
public class VMMismatchException extends RuntimeException {

    /** Sin detalle. */
    public VMMismatchException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public VMMismatchException(String s) {
        super(s);
    }
}
