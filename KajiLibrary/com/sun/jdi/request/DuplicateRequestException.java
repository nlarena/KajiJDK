package com.sun.jdi.request;

/**
 * Se pidio algo que ya estaba pedido y no admite duplicados.
 *
 * <p>Pasa con los pedidos de paso: solo puede haber uno por hilo.
 *
 * @since 1.3
 */
public class DuplicateRequestException extends RuntimeException {

    /** Sin detalle. */
    public DuplicateRequestException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public DuplicateRequestException(String s) {
        super(s);
    }
}
