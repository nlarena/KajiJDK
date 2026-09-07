package com.sun.jdi;

/**
 * El marco no deja hacer esa operacion.
 *
 * <p>Pasa con los marcos nativos y con los de un hilo virtual montado: no hay pila de Java que
 * manipular.
 *
 * @since 1.3
 */
public class OpaqueFrameException extends RuntimeException {

    /** Sin detalle. */
    public OpaqueFrameException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public OpaqueFrameException(String s) {
        super(s);
    }
}
