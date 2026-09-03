package org.w3c.dom.ls;

import org.w3c.dom.events.Event;

/**
 * KajiLibrary's org.w3c.dom.ls.LSProgressEvent -- avance de una carga.
 *
 * <p>Se manda cada tanto mientras un {@link LSParser} asincronico trabaja, para poder mostrar una
 * barra de progreso.
 *
 * <p>Los dos numeros estan en bytes del documento fuente, no en nodos del arbol. Es la unidad
 * correcta --el analizador sabe cuanto leyo, no cuanto le falta por construir-- y tiene una
 * consecuencia que conviene tener presente: {@link #getTotalSize} <b>no siempre se conoce</b>. Un
 * documento que llega por un flujo sin largo declarado no tiene total, y la especificacion no define
 * un valor para ese caso, asi que quien dibuja la barra tiene que estar preparado para no tenerlo.
 *
 * <p>La frecuencia con que se manda queda a criterio de la implementacion. No hay forma de pedir una
 * granularidad, y por eso no sirve para contar: sirve para mostrar.
 */
public interface LSProgressEvent extends Event {

    /** De donde se esta cargando. */
    LSInput getInput();

    /** Cuantos bytes se leyeron. */
    int getPosition();

    /** Cuantos hay en total, si se sabe. Ver la nota de la clase. */
    int getTotalSize();
}
