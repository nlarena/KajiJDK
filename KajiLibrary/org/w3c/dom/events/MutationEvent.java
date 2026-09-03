package org.w3c.dom.events;

import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.events.MutationEvent -- el documento cambio.
 *
 * <p>Es el unico evento del DOM que no viene de una persona: lo dispara el propio arbol cuando se le
 * agrega, saca o modifica algo. Sirve para mantener sincronizada una vista sin tener que revisar el
 * documento entero.
 *
 * <h2>relatedNode es el que NO es el objetivo</h2>
 *
 * <p>Y cual es depende del evento, que es lo que confunde: en {@code DOMNodeInserted} el objetivo es
 * el nodo insertado y el relacionado es su <b>padre nuevo</b>; en {@code DOMAttrModified} el objetivo
 * es el elemento y el relacionado es el <b>atributo</b>.
 *
 * <p>Los tres campos de valor solo tienen sentido para cambios de atributo o de texto, y para el
 * resto son null. {@link #getAttrChange()} dice si el atributo se agrego, se saco o se cambio.
 */
public interface MutationEvent extends Event {

    /** El atributo se modifico. */
    short MODIFICATION = 1;

    /** El atributo se agrego. */
    short ADDITION = 2;

    /** El atributo se saco. */
    short REMOVAL = 3;

    /** El otro nodo involucrado. Cual es depende del evento; ver la nota de la clase. */
    Node getRelatedNode();

    /** El valor anterior, o null si el evento no cambia un valor. */
    String getPrevValue();

    /** El valor nuevo, o null. */
    String getNewValue();

    /** El nombre del atributo que cambio, o null. */
    String getAttrName();

    /** Cual de los tres cambios fue. Solo tiene sentido en un cambio de atributo. */
    short getAttrChange();

    /** Inicializa un evento de mutacion recien creado. */
    void initMutationEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg,
        Node relatedNodeArg, String prevValueArg, String newValueArg, String attrNameArg,
        short attrChangeArg);
}
