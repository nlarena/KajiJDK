package com.sun.jdi;

/**
 * Se corto la conexion con la maquina depurada.
 *
 * <p>No es verificada a proposito: puede pasar en cualquier llamada de JDI, y declararla en todas
 * obligaria a envolver cada linea de un depurador en un {@code try}.
 *
 * @since 1.3
 */
public class VMDisconnectedException extends RuntimeException {

    /** Sin detalle. */
    public VMDisconnectedException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public VMDisconnectedException(String s) {
        super(s);
    }
}
