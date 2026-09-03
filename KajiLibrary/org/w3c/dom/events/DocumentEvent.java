package org.w3c.dom.events;

import org.w3c.dom.DOMException;

/**
 * KajiLibrary's org.w3c.dom.events.DocumentEvent -- la fabrica de eventos.
 *
 * <p>La implementa el {@code Document}. Es la unica forma de crear un evento: no hay constructores.
 *
 * <p>El evento sale <b>vacio</b> y hay que inicializarlo con el {@code init*} que le corresponda a su
 * tipo antes de despacharlo. Es en dos pasos porque la fabrica toma un nombre de interfaz y no puede
 * saber que argumentos lleva cada una.
 */
public interface DocumentEvent {

    /**
     * Un evento del tipo pedido, sin inicializar.
     *
     * @param eventType el nombre de la <b>interfaz</b>, no del evento: {@code "MouseEvents"},
     *     {@code "MutationEvents"}, {@code "UIEvents"}, {@code "Events"}. En plural, que es como lo
     *     escribe el estandar y es facil de equivocar
     * @throws DOMException {@code NOT_SUPPORTED_ERR} si esa interfaz no esta implementada
     */
    Event createEvent(String eventType) throws DOMException;
}
