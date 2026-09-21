package org.w3c.dom.xpath;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathExpression -- an expression already compiled.
 *
 * <p>It exists to be able to <b>reuse</b> it: compiling XPath costs, and evaluating the same
 * expression over a thousand nodes has no reason to pay for it a thousand times.
 */
public interface XPathExpression {

    /**
     * It evaluates the expression with that node as the context.
     *
     * @param type   the type of result wanted, one of the constants of {@link XPathResult}. With
     *     {@code ANY_TYPE} one receives whatever the expression naturally gives
     * @param result an {@link XPathResult} to reuse, or null for one to be created. Reusing avoids
     *     one allocation per evaluation; the implementation may ignore it and return another
     * @throws XPathException {@code TYPE_ERR} if the result cannot be converted to that type
     * @throws DOMException {@code WRONG_DOCUMENT_ERR} if the node is from another document
     */
    Object evaluate(Node contextNode, short type, Object result)
        throws XPathException, DOMException;
}
