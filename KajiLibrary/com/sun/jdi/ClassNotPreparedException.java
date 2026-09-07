package com.sun.jdi;

/**
 * La clase esta cargada pero todavia no preparada.
 *
 * <p>Entre cargar y preparar hay un paso: la VM tiene que ligar la clase y darles valor a sus
 * campos estaticos. Antes de eso no hay estado que leer.
 *
 * @since 1.3
 */
public class ClassNotPreparedException extends RuntimeException {

    /** Sin detalle. */
    public ClassNotPreparedException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public ClassNotPreparedException(String s) {
        super(s);
    }
}
