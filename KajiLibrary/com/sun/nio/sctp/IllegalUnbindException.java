package com.sun.nio.sctp;

/**
 * Se pidio desligar una direccion que no se puede desligar: la ultima que queda, o una que nunca estuvo ligada.
 */
public class IllegalUnbindException extends IllegalStateException {

    private static final long serialVersionUID = 2493124086598L;

    /** Sin detalle. */
    public IllegalUnbindException() {
        super();
    }

    /** Con un mensaje que explique el caso. */
    public IllegalUnbindException(String msg) {
        super(msg);
    }
}
