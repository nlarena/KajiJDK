package com.sun.jdi;

/**
 * NativeMethodException de la maquina depurada.
 *
 * @since 1.3
 */
public class NativeMethodException extends OpaqueFrameException {

    /** Sin detalle. */
    public NativeMethodException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public NativeMethodException(String s) {
        super(s);
    }
}
