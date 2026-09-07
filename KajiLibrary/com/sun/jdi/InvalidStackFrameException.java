package com.sun.jdi;

/**
 * El marco ya no vale: el hilo se reanudo desde que se lo obtuvo.
 *
 * @since 1.3
 */
public class InvalidStackFrameException extends RuntimeException {

    /** Sin detalle. */
    public InvalidStackFrameException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public InvalidStackFrameException(String s) {
        super(s);
    }
}
