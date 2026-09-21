package org.w3c.dom.xpath;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathEvaluator -- it evaluates XPath over a document.
 *
 * <p>The {@code Document} implements it. It has both roads: compiling once and evaluating many
 * times ({@link #createExpression}), or evaluating in one go ({@link #evaluate}). The second is
 * convenient and the first is the one to use in a loop.
 */
public interface XPathEvaluator {

    /**
     * It compiles the expression.
     *
     * @throws XPathException {@code INVALID_EXPRESSION_ERR} if it is not valid XPath
     * @throws DOMException {@code NAMESPACE_ERR} if it uses a prefix the resolver does not know
     */
    XPathExpression createExpression(String expression, XPathNSResolver resolver)
        throws XPathException, DOMException;

    /**
     * A resolver that uses the prefixes <b>declared on that node</b> and its ancestors.
     *
     * <p>It is the shortcut for the common case: querying a document with its own prefixes.
     */
    XPathNSResolver createNSResolver(Node nodeResolver);

    /**
     * It compiles and evaluates in one go. See {@link XPathExpression#evaluate} for the arguments.
     */
    Object evaluate(String expression, Node contextNode, XPathNSResolver resolver, short type,
        Object result) throws XPathException, DOMException;
}
