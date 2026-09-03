package org.w3c.dom.xpath;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathEvaluator -- evalua XPath sobre un documento.
 *
 * <p>La implementa el {@code Document}. Tiene los dos caminos: compilar una vez y evaluar muchas
 * ({@link #createExpression}), o evaluar de una sola vez ({@link #evaluate}). El segundo es comodo y
 * el primero es el que hay que usar en un bucle.
 */
public interface XPathEvaluator {

    /**
     * Compila la expresion.
     *
     * @throws XPathException {@code INVALID_EXPRESSION_ERR} si no es XPath valido
     * @throws DOMException {@code NAMESPACE_ERR} si usa un prefijo que el resolvedor no conoce
     */
    XPathExpression createExpression(String expression, XPathNSResolver resolver)
        throws XPathException, DOMException;

    /**
     * Un resolvedor que usa los prefijos <b>declarados en ese nodo</b> y sus antepasados.
     *
     * <p>Es el atajo para el caso comun: consultar un documento con sus propios prefijos.
     */
    XPathNSResolver createNSResolver(Node nodeResolver);

    /** Compila y evalua de una vez. Ver {@link XPathExpression#evaluate} para los argumentos. */
    Object evaluate(String expression, Node contextNode, XPathNSResolver resolver, short type,
        Object result) throws XPathException, DOMException;
}
