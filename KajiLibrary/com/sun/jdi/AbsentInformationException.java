package com.sun.jdi;

/**
 * La clase no trae la informacion de depuracion que se pidio.
 *
 * <p>Los nombres de las variables locales y los numeros de linea son opcionales: se emiten solo si
 * se compilo pidiendolos. Recibir esto no significa que el programa este mal.
 *
 * @since 1.3
 */
public class AbsentInformationException extends Exception {

    /** Sin detalle. */
    public AbsentInformationException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public AbsentInformationException(String s) {
        super(s);
    }
}
