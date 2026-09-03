package org.w3c.dom.xpath;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathExpression -- una expresion ya compilada.
 *
 * <p>Existe para poder <b>reusarla</b>: compilar XPath cuesta, y evaluar la misma expresion sobre mil
 * nodos no tiene por que pagarlo mil veces.
 */
public interface XPathExpression {

    /**
     * Evalua la expresion con ese nodo como contexto.
     *
     * @param type   el tipo de resultado que se quiere, una de las constantes de
     *     {@link XPathResult}. Con {@code ANY_TYPE} se recibe lo que la expresion de naturalmente
     * @param result un {@link XPathResult} para reusar, o null para que se cree uno. Reusar evita
     *     una asignacion por evaluacion; la implementacion puede ignorarlo y devolver otro
     * @throws XPathException {@code TYPE_ERR} si el resultado no se puede convertir a ese tipo
     * @throws DOMException {@code WRONG_DOCUMENT_ERR} si el nodo es de otro documento
     */
    Object evaluate(Node contextNode, short type, Object result)
        throws XPathException, DOMException;
}
