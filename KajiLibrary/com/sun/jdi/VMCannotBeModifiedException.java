package com.sun.jdi;

/**
 * La maquina depurada esta en modo de solo lectura.
 *
 * <p>Se puede conectar un depurador que solo observa, y ahi todo lo que cambie el estado del otro
 * lado --escribir un campo, invocar un metodo-- falla con esto.
 *
 * @since 1.3
 */
public class VMCannotBeModifiedException extends UnsupportedOperationException {

    /** Sin detalle. */
    public VMCannotBeModifiedException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public VMCannotBeModifiedException(String s) {
        super(s);
    }
}
