package com.sun.jdi;

/**
 * VMOutOfMemoryException de la maquina depurada.
 *
 * @since 1.3
 */
public class VMOutOfMemoryException extends RuntimeException {

    /** Sin detalle. */
    public VMOutOfMemoryException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public VMOutOfMemoryException(String s) {
        super(s);
    }
}
