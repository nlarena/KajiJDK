package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DocumentFragment -- un contenedor liviano para mover varios nodos de una.
 *
 * <p>No declara miembros, pero no es decorativo: tiene un **comportamiento propio en la insercion**
 * que no esta escrito en ninguna firma. Cuando un fragmento se pasa a
 * {@link Node#appendChild} o a {@link Node#insertBefore}, lo que se inserta no es el fragmento sino
 * **sus hijos**, en orden, y el fragmento queda vacio. Es la unica manera que da el DOM de mover un
 * grupo de hermanos en una sola operacion, y la razon por la que existe: sin el, insertar n nodos
 * son n operaciones, con n notificaciones a quien este observando y n oportunidades de dejar el
 * arbol en un estado intermedio raro.
 *
 * <p>De ahi tambien que sea un {@link Node} sin ser parte del documento: no tiene padre, no se
 * serializa, y {@link Node#getNodeName} devuelve {@code "#document-fragment"}.
 *
 * <p>Interfaz declarada entera; el JDK tampoco declara miembros aca.
 */
public interface DocumentFragment extends Node {
}
