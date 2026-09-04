package com.sun.nio.sctp;

/**
 * Se pidio recibir por un canal que no puede hacerlo ahora — por ejemplo un {@link SctpMultiChannel} sin asociaciones.
 */
public class IllegalReceiveException extends IllegalStateException {

    private static final long serialVersionUID = 742758972917L;

    /** Sin detalle. */
    public IllegalReceiveException() {
        super();
    }

    /** Con un mensaje que explique el caso. */
    public IllegalReceiveException(String msg) {
        super(msg);
    }
}
