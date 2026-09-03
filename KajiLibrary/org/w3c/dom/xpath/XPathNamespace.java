package org.w3c.dom.xpath;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathNamespace -- un nodo de espacio de nombres.
 *
 * <p>XPath tiene siete tipos de nodo y el DOM tiene doce, pero <b>no se superponen del todo</b>: el
 * nodo de espacio de nombres de XPath no existe en el DOM. Esta interfaz lo agrega para que el eje
 * {@code namespace::} pueda devolver algo.
 *
 * <p>De ahi sale que sea un {@code Node} raro: su {@code nodeName} es el prefijo, su
 * {@code nodeValue} es el URI, y casi todo lo demas --hijos, atributos, padre-- es null. No es un
 * nodo del arbol; es una vista de una declaracion que en el DOM vive como atributo.
 *
 * <p>Su {@code getNodeType()} devuelve {@link #XPATH_NAMESPACE_NODE}, que es 13: uno mas que los doce
 * del DOM, elegido justamente para no chocar.
 */
public interface XPathNamespace extends Node {

    /** El tipo de nodo, 13: uno mas que los doce del DOM. */
    short XPATH_NAMESPACE_NODE = 13;

    /** El elemento donde esta declarado el espacio de nombres. */
    Element getOwnerElement();
}
