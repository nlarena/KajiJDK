package org.w3c.dom.events;

import org.w3c.dom.views.AbstractView;

/**
 * KajiLibrary's org.w3c.dom.events.MouseEvent -- un evento de puntero.
 *
 * <h2>Dos sistemas de coordenadas, y ninguno es el que uno quiere</h2>
 *
 * <ul>
 *   <li><b>screen</b> -- relativo a la pantalla fisica. Sirve para posicionar algo fuera del
 *       documento; adentro no dice nada.
 *   <li><b>client</b> -- relativo al area visible del cliente. <b>No incluye el desplazamiento</b>:
 *       el mismo punto del documento da coordenadas distintas si la pagina esta scrolleada.
 * </ul>
 *
 * <p>La coordenada relativa al documento --la que casi siempre se busca-- <b>no esta</b> en este
 * nivel del DOM; hay que sumarle el desplazamiento a mano.
 *
 * <h2>relatedTarget cambia de sentido segun el evento</h2>
 *
 * <p>Para {@code mouseover} es de <b>donde venia</b> el puntero; para {@code mouseout}, a <b>donde
 * va</b>. Para los demas es null. Leerlo sin mirar el tipo del evento da el nodo equivocado la mitad
 * de las veces.
 */
public interface MouseEvent extends UIEvent {

    /** X relativa a la pantalla. */
    int getScreenX();

    /** Y relativa a la pantalla. */
    int getScreenY();

    /** X relativa al area visible. No incluye el desplazamiento; ver la nota de la clase. */
    int getClientX();

    /** Y relativa al area visible. */
    int getClientY();

    /** Si Control estaba apretada. */
    boolean getCtrlKey();

    /** Si Shift estaba apretada. */
    boolean getShiftKey();

    /** Si Alt estaba apretada. */
    boolean getAltKey();

    /** Si Meta estaba apretada. */
    boolean getMetaKey();

    /**
     * Que boton: 0 el principal, 1 el del medio, 2 el secundario.
     *
     * <p>Son posiciones logicas, no fisicas: en un raton para zurdos el 0 es el de la derecha.
     */
    short getButton();

    /** El otro nodo involucrado. Su sentido depende del evento; ver la nota de la clase. */
    EventTarget getRelatedTarget();

    /** Inicializa un evento de puntero recien creado. */
    void initMouseEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg,
        AbstractView viewArg, int detailArg, int screenXArg, int screenYArg, int clientXArg,
        int clientYArg, boolean ctrlKeyArg, boolean altKeyArg, boolean shiftKeyArg,
        boolean metaKeyArg, short buttonArg, EventTarget relatedTargetArg);
}
