package com.sun.jdi.connect;

import java.io.IOException;

/**
 * Se vencio el plazo de una operacion de transporte.
 *
 * <p>Sale de los tres lugares donde un conector espera al otro extremo: adjuntarse, empezar a
 * escuchar, y aceptar. Es una {@link IOException} porque para el que llama es eso: un fallo del
 * medio, no del protocolo.
 *
 * <p>Vale distinguirla de "no se pudo conectar": aca el plazo es del **cliente**, y volver a
 * intentar con uno mas largo puede funcionar.
 */
public class TransportTimeoutException extends IOException {

    private static final long serialVersionUID = 4107035242623365074L;

    /** Un vencimiento sin detalle. */
    public TransportTimeoutException() {
        super();
    }

    /**
     * Un vencimiento con detalle.
     *
     * @param message el detalle
     */
    public TransportTimeoutException(String message) {
        super(message);
    }
}
