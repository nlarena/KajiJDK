package com.sun.jdi.request;

/**
 * El pedido no esta en el estado que la operacion necesita.
 *
 * <p>Casi siempre significa lo mismo: se intento poner un filtro con el pedido ya habilitado.
 *
 * @since 1.3
 */
public class InvalidRequestStateException extends RuntimeException {

    /** Sin detalle. */
    public InvalidRequestStateException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public InvalidRequestStateException(String s) {
        super(s);
    }
}
