package org.w3c.dom.events;

/**
 * KajiLibrary's org.w3c.dom.events.EventException -- un evento que no se puede despachar.
 *
 * <p>Tiene un solo codigo, y eso ya dice como esta pensado el modulo: casi todo lo que puede salir
 * mal en el despacho es responsabilidad de los escuchas, no del despachador. Lo unico que el
 * despachador puede rechazar es un evento <b>sin tipo</b> -- sin el no hay a quien entregarselo.
 *
 * <p>No chequeada y con el codigo en un campo publico, por la misma convencion del DOM que
 * {@code DOMException}.
 */
public class EventException extends RuntimeException {

    private static final long serialVersionUID = 3728411136506952248L;

    /** El tipo del evento es null o vacio: no hay a quien entregarselo. */
    public static final short UNSPECIFIED_EVENT_TYPE_ERR = 0;

    /** Cual. Publico por la convencion del DOM. */
    public short code;

    public EventException(short code, String message) {
        super(message);
        this.code = code;
    }
}
