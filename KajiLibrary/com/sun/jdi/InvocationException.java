package com.sun.jdi;

/**
 * El metodo que se invoco del otro lado lanzo una excepcion.
 *
 * <p>{@link #exception} devuelve la excepcion <strong>de la otra VM</strong>, como
 * {@link ObjectReference}. No se puede lanzar aca ni convertir: es un objeto de otro proceso.
 *
 * @since 1.3
 */
public class InvocationException extends Exception {

    private final ObjectReference exception;

    /**
     * Con la excepcion que lanzo el metodo del otro lado.
     *
     * <p>El mensaje es fijo, como en el JDK: el detalle esta en la excepcion misma, que vive en la
     * otra VM y no se puede formatear desde aca.
     *
     * @param exception la excepcion de la maquina depurada
     */
    public InvocationException(ObjectReference exception) {
        super("Exception occurred in target VM");
        this.exception = exception;
    }

    /**
     * La excepcion que lanzo el metodo, como objeto de la otra VM.
     *
     * @return la excepcion
     */
    public ObjectReference exception() {
        return exception;
    }
}
