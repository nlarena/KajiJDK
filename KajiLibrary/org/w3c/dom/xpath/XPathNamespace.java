package org.w3c.dom.xpath;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathNamespace -- a namespace node.
 *
 * <p>XPath has seven node types and the DOM has twelve, but <b>they do not overlap entirely</b>:
 * the XPath namespace node does not exist in the DOM. This interface adds it so that the {@code
 * namespace::} axis can return something.
 *
 * <p>Hence it is an odd {@code Node}: its {@code nodeName} is the prefix, its {@code nodeValue} is
 * the URI, and almost everything else --children, attributes, parent-- is null. It is not a node of
 * the tree; it is a view of a declaration that in the DOM lives as an attribute.
 *
 * <p>Its {@code getNodeType()} returns {@link #XPATH_NAMESPACE_NODE}, which is 13: one more than
 * the twelve of the DOM, chosen precisely so as not to clash.
 */
public interface XPathNamespace extends Node {

    /** The node type, 13: one more than the twelve of the DOM. */
    short XPATH_NAMESPACE_NODE = 13;

    /** The element where the namespace is declared. */
    Element getOwnerElement();
}
