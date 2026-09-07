package com.sun.jdi;

/**
 * La operacion necesita el hilo suspendido y el hilo esta corriendo.
 *
 * <p>Leer la pila de un hilo en movimiento no da un resultado incompleto: da uno sin significado,
 * porque los marcos cambian mientras se los recorre.
 *
 * @since 1.3
 */
public class IncompatibleThreadStateException extends Exception {

    /** Sin detalle. */
    public IncompatibleThreadStateException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public IncompatibleThreadStateException(String s) {
        super(s);
    }
}
