package javax.xml.xpath;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.xpath.XPathFunctionResolver -- where the custom functions come from.
 *
 * <p>It is asked by name <b>and number of arguments</b>, because in XPath two functions with the
 * same name and a different arity are different functions. There is no way to enumerate what a
 * resolver offers: you can only ask it about one in particular.
 *
 * <p>The name comes qualified with a namespace, and that is not decoration: a custom function
 * <b>has</b> to be in a namespace of its own. Without a prefix, the name falls into the one of
 * XPath's built-in functions, where nothing can be added.
 */
public interface XPathFunctionResolver {

    /**
     * The function with that name and that arity.
     *
     * @return null if this resolver does not know it
     */
    XPathFunction resolveFunction(QName functionName, int arity);
}
