package com.sun.nio.sctp;

/**
 * El numero de flujo de un {@link MessageInfo} esta fuera del rango que la asociacion negocio.<p>Es una {@link IllegalArgumentException} y no una de E/S porque el error es del programa, no de la red: el rango se conoce de antemano en {@link Association#maxOutboundStreams}.
 */
public class InvalidStreamException extends IllegalArgumentException {

    private static final long serialVersionUID = 29332933412071L;

    /** Sin detalle. */
    public InvalidStreamException() {
        super();
    }

    /** Con un mensaje que explique el caso. */
    public InvalidStreamException(String msg) {
        super(msg);
    }
}
