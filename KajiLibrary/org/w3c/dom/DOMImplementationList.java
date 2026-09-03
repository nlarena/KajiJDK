package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMImplementationList -- una lista ordenada de implementaciones.
 *
 * <p>La devuelve {@link DOMImplementationSource#getDOMImplementationList} cuando mas de una
 * implementacion dice soportar lo que se pidio. Misma forma minima que {@link NodeList} --e
 * indexada desde cero-- y por la misma razon: el DOM no se apoya en las colecciones de ningun
 * lenguaje.
 *
 * <p>Interfaz declarada entera.
 */
public interface DOMImplementationList {

    /** La implementacion en esa posicion, o {@code null} si el indice se fue de rango. */
    public DOMImplementation item(int index);

    /** Cuantas hay. */
    public int getLength();
}
