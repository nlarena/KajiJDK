package javax.xml.xpath;

import java.util.List;

/**
 * KajiLibrary's javax.xml.xpath.XPathFunction -- a custom function, callable from the expression.
 *
 * <p>XPath 1.0 ships some thirty functions and there is no way to write a new one <b>in</b> XPath.
 * This interface is the way out: a function written in Java that the expression calls by name.
 *
 * <p>The arguments arrive as a list of {@code Object} and the mapping matters: an XPath number
 * arrives as a {@code Double} --XPath 1.0 has no integers--, a node-set as a {@code NodeList}, and
 * a string as a {@code String}. Returning something that is not one of those types leaves the
 * result undefined.
 */
public interface XPathFunction {

    /**
     * Runs the function.
     *
     * @param args the arguments, already converted to the XPath types
     * @throws XPathFunctionException if the function fails
     */
    Object evaluate(List<?> args) throws XPathFunctionException;
}
