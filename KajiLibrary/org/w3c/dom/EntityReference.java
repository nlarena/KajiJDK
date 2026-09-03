package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.EntityReference -- un {@code &nombre;} que quedo sin expandir.
 *
 * <p>Sin miembros propios: lo que dice es **que el arbol conservo la referencia** en vez de haberla
 * reemplazado por su contenido. Un parser que expande entidades no produce ninguno de estos nodos y
 * un arbol perfectamente valido puede no tener ni uno; que aparezcan o no es decision de la
 * implementacion, y por eso el codigo que recorre un DOM tiene que tolerar las dos formas.
 *
 * <p>Cuando aparecen, sus hijos son una copia del contenido de la {@link Entity} y son **de solo
 * lectura**, junto con todo lo que cuelgue de ahi: cambiar la expansion de una referencia y no la de
 * otra dejaria dos copias del mismo texto diciendo cosas distintas. Para cambiar el texto hay que
 * reemplazar la referencia entera.
 *
 * <p>Interfaz declarada entera; el JDK tampoco declara miembros aca.
 */
public interface EntityReference extends Node {
}
