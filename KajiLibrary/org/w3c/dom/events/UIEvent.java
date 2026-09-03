package org.w3c.dom.events;

import org.w3c.dom.views.AbstractView;

/**
 * KajiLibrary's org.w3c.dom.events.UIEvent -- un evento de interfaz de usuario.
 *
 * <p>Agrega dos cosas sobre {@link Event}: en <b>que vista</b> ocurrio, que es la unica razon por la
 * que existe {@code org.w3c.dom.views}, y un contador cuyo significado depende del evento --para un
 * clic, cuantos clics seguidos; para un desplazamiento, cuantas lineas--.
 */
public interface UIEvent extends Event {

    /** La vista donde ocurrio, o null. */
    AbstractView getView();

    /** Un numero cuyo significado depende del tipo de evento. Ver la nota de la clase. */
    int getDetail();

    /** Inicializa un evento de interfaz recien creado. */
    void initUIEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg,
        AbstractView viewArg, int detailArg);
}
