package org.w3c.dom.ls;

import org.w3c.dom.Document;
import org.w3c.dom.events.Event;

/**
 * KajiLibrary's org.w3c.dom.ls.LSLoadEvent -- termino de cargarse un documento.
 *
 * <p>Es como devuelve su resultado un {@link LSParser} asincronico: la llamada a {@code parse} vuelve
 * enseguida sin documento, y el documento llega mas tarde aca.
 *
 * <p>Que sea un {@code Event} del DOM y no una interfaz de callback propia tiene una consecuencia
 * practica: se escucha con {@code addEventListener} sobre el analizador, y por lo tanto puede
 * haber <b>varios</b> interesados en la misma carga sin que ninguno sepa de los otros.
 *
 * <p>{@link #getInput} viene junto con el documento a proposito: con varias cargas en vuelo, el
 * evento solo no alcanzaria para saber cual termino.
 */
public interface LSLoadEvent extends Event {

    /** El documento que se termino de cargar. */
    Document getNewDocument();

    /** De donde se cargo; ver la nota de la clase sobre por que hace falta. */
    LSInput getInput();
}
