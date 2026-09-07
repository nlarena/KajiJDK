package com.sun.jdi;

/**
 * InternalException de la maquina depurada.
 *
 * @since 1.3
 */
public class InternalException extends RuntimeException {

    private final int errorCode;

    /** Sin detalle. */
    public InternalException() {
        super();
        this.errorCode = 0;
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public InternalException(String s) {
        super(s);
        this.errorCode = 0;
    }

    /**
     * Con el codigo de error de la capa de transporte.
     *
     * @param errorCode el codigo
     */
    public InternalException(int errorCode) {
        super();
        this.errorCode = errorCode;
    }

    /**
     * Con un mensaje y el codigo de error.
     *
     * @param s el mensaje
     * @param errorCode el codigo
     */
    public InternalException(String s, int errorCode) {
        super(s);
        this.errorCode = errorCode;
    }

    /**
     * El codigo de error de la capa de transporte, o cero si no habia.
     *
     * @return el codigo
     */
    public int errorCode() {
        return errorCode;
    }
}
