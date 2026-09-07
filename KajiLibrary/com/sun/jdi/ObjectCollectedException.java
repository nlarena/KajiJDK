package com.sun.jdi;

/**
 * El objeto del otro lado ya no existe: el recolector se lo llevo.
 *
 * <p>Es la excepcion que hace visible que estos objetos son <strong>reflejos</strong>. Entre que se
 * obtiene una referencia y se la usa, la otra VM sigue corriendo. Para evitarlo esta
 * {@code ObjectReference.disableCollection}.
 *
 * @since 1.3
 */
public class ObjectCollectedException extends RuntimeException {

    /** Sin detalle. */
    public ObjectCollectedException() {
        super();
    }

    /**
     * Con un mensaje.
     *
     * @param s el mensaje
     */
    public ObjectCollectedException(String s) {
        super(s);
    }
}
