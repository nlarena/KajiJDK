package javax.xml.xpath;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.xpath.XPathVariableResolver -- where the variables come from.
 *
 * <p>An expression can name variables with {@code $name}, and XPath has no way to assign them: they
 * are resolved from the outside, with this.
 *
 * <p>It is what avoids building expressions by concatenating text, which is XPath's equivalent of
 * SQL injection: a value with a quote inside changes what the expression selects. With a variable,
 * the value never goes through the parser.
 *
 * <p>It is consulted <b>on every evaluation</b>, not when compiling. That is why the same compiled
 * expression serves for many different values, which is the other reason to use them.
 */
public interface XPathVariableResolver {

    /**
     * The value of that variable.
     *
     * @return null if this resolver does not know it
     */
    Object resolveVariable(QName variableName);
}
