package com.sun.jdi.connect.spi;

import java.io.IOException;

/**
 * La conexión se cerró o se rompió mientras se la usaba.
 *
 * <p>Es una {@link IOException} y no algo propio porque para quien la recibe es exactamente eso: una
 * operación de entrada/salida que no se pudo completar. Lo que agrega sobre una {@code IOException}
 * cualquiera es la <em>causa</em>, y es una distinción que importa — un fin de flujo ordenado se
 * reporta con un paquete de largo cero desde {@link Connection#readPacket}, no con esta excepción.
 * Verla significa que la conexión ya no sirve, no que el otro lado terminó de hablar.
 */
public class ClosedConnectionException extends IOException {

    private static final long serialVersionUID = 3877032124297204774L;

    /** Sin detalle. */
    public ClosedConnectionException() {
        super();
    }

    /** Con un mensaje que explique qué la cerró. */
    public ClosedConnectionException(String message) {
        super(message);
    }
}
