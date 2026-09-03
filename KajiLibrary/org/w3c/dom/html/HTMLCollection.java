package org.w3c.dom.html;

import org.w3c.dom.Node;

/**
 * Una lista de elementos indexable por posicion y por nombre.
 *
 * <p>Es **viva**, como las de {@link HTMLDocument}: lo que devuelve refleja el arbol en el momento
 * de la consulta.
 *
 * <p>`namedItem` busca primero por `id` y despues por `name`, en ese orden. Importa cuando los dos
 * atributos existen y no coinciden: gana el `id`.
 */
public interface HTMLCollection {

    /** La cantidad. */
    int getLength();

    /** El elemento en esa posicion, o nulo si el indice esta fuera de rango. */
    Node item(int index);

    /** El elemento con ese `id`, o en su defecto con ese `name`; nulo si no hay. */
    Node namedItem(String name);
}
