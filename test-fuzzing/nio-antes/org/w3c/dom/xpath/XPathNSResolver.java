package org.w3c.dom.xpath;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathNSResolver -- it translates prefixes to namespaces.
 *
 * <p>It is needed because the prefixes of an XPath expression <b>do not have to be those of the
 * document</b>: whoever queries writes the expression, somebody else wrote the document, and the
 * two may use different prefixes for the same namespace -- or the same prefix for two different
 * ones. What is compared is the namespace, and this resolver is the one that says it.
 */
public interface XPathNSResolver {

    /**
     * The namespace of that prefix, or null if it does not know it.
     *
     * @param prefix the prefix, or null for the default namespace
     */
    String lookupNamespaceURI(String prefix);
}
