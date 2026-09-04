package com.sun.source.tree;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * El alcance lexico en un punto del programa, para preguntar que nombres son visibles.
 *
 * <p>Como {@link LineMap}, no es un nodo: es una vista *resuelta* del arbol, y por eso devuelve
 * {@code Element} del modelo de elementos y no nodos del arbol. La cadena de
 * {@link #getEnclosingScope} es lo que hace la busqueda de un nombre.
 */
public interface Scope {

    Scope getEnclosingScope();

    TypeElement getEnclosingClass();

    ExecutableElement getEnclosingMethod();

    Iterable<? extends Element> getLocalElements();
}
