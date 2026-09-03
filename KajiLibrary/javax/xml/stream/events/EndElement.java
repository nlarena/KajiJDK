package javax.xml.stream.events;

import java.util.Iterator;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.stream.events.EndElement -- el cierre de un elemento.
 *
 * <h2>Por que un cierre trae espacios de nombres</h2>
 *
 * <p>La etiqueta {@code </a:x>} no declara nada, asi que la pregunta legitima es que hace
 * {@link #getNamespaces()} aca. La respuesta es que lo que devuelve no son declaraciones nuevas
 * sino las que <b>dejan de valer</b> en este punto: las que el {@link StartElement}
 * correspondiente habia introducido y que, al cerrarse el elemento, salen de alcance.
 *
 * <p>Sirve para lo unico que hace falta al cerrar: un escritor que mantiene su propia pila de
 * prefijos necesita saber cuales desapilar, y un consumidor que va armando un modelo necesita
 * saber cuando un prefijo vuelve a significar otra cosa. Sin esto habria que llevar la pila por
 * fuera, que es exactamente el estado que el modelo de eventos existe para no obligar a llevar.
 */
public interface EndElement extends XMLEvent {

    /**
     * El nombre del elemento que se cierra.
     *
     * <p>Es el mismo {@link QName} que traia el {@link StartElement}: mismo espacio de nombres,
     * mismo nombre local. El prefijo tambien coincide, porque XML exige que las etiquetas se
     * escriban igual, pero recordar que el prefijo no entra en {@link QName#equals}.
     *
     * @return el nombre calificado; nunca null
     */
    QName getName();

    /**
     * Los espacios de nombres que salen de alcance al cerrar este elemento.
     *
     * @return un iterador de {@link Namespace}; vacio si el elemento no habia declarado ninguno,
     *     nunca null
     */
    Iterator<Namespace> getNamespaces();
}
