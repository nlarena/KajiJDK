package org.w3c.dom.events;

/**
 * KajiLibrary's org.w3c.dom.events.EventListener -- quien recibe un evento.
 *
 * <p>Un solo metodo, y sin valor de retorno a proposito: un escucha <b>no decide</b> si el evento
 * sigue. Para eso estan {@code Event.stopPropagation()} y {@code Event.preventDefault()}, que dicen
 * dos cosas distintas y se confunden -- ver {@link Event}.
 */
public interface EventListener {

    /** Recibe el evento. */
    void handleEvent(Event evt);
}
