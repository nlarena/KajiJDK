package com.sun.tools.attach;

import java.io.IOException;

/**
 * La VM destino recibio la operacion, la ejecuto y contesto que fallo.
 *
 * <p>Es una {@link IOException} y ahi esta el matiz que vale: hereda de la misma clase que las
 * fallas de comunicacion, pero significa lo contrario. Una {@code IOException} comun quiere decir
 * que el canal se rompio y no se sabe que paso del otro lado; esta quiere decir que el canal
 * funciono perfecto y la respuesta fue "no". El mensaje viene de la VM destino, no de esta.
 */
public class AttachOperationFailedException extends IOException {

    private static final long serialVersionUID = 2140308168167478043L;

    /** Con el mensaje que mando la VM destino. */
    public AttachOperationFailedException(String message) {
        super(message);
    }
}
