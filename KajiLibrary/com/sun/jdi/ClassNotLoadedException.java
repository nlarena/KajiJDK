package com.sun.jdi;

/**
 * ClassNotLoadedException de la maquina depurada.
 *
 * @since 1.3
 */
public class ClassNotLoadedException extends Exception {

    private final String className;

    /**
     * Con el nombre de la clase que falta.
     *
     * <p>El nombre NO va al mensaje: el JDK lo guarda aparte y deja el mensaje vacio. Se reproduce
     * porque un programa que formatea el mensaje veria otra cosa.
     *
     * @param className el nombre de la clase
     */
    public ClassNotLoadedException(String className) {
        super();
        this.className = className;
    }

    /**
     * Con el nombre de la clase y un mensaje.
     *
     * @param className el nombre de la clase
     * @param message el mensaje
     */
    public ClassNotLoadedException(String className, String message) {
        super(message);
        this.className = className;
    }

    /**
     * El nombre de la clase que falta.
     *
     * @return el nombre
     */
    public String className() {
        return className;
    }
}
