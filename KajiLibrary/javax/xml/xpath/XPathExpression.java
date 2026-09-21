package javax.xml.xpath;

import javax.xml.namespace.QName;
import org.xml.sax.InputSource;

/**
 * KajiLibrary's javax.xml.xpath.XPathExpression -- an already compiled expression.
 *
 * <p>Compiling is expensive and evaluating is cheap, so an expression applied many times is
 * compiled once. It is the same relation as between {@code Pattern} and {@code Matcher}, or between
 * a compiled schema and its validators.
 *
 * <p>The two families of {@code evaluate} differ in where the document comes from: the ones taking
 * {@code Object} work on a tree that already exists, and the ones taking {@link InputSource}
 * <b>read and parse</b> the document on every call. The second is convenient and expensive: to
 * evaluate several expressions on the same document, parse it once and use the first.
 *
 * <p>The overloads without a {@link QName} return {@code String}, which is asking for
 * {@link XPathConstants#STRING}. The {@code evaluateExpression} methods are the modern way: you ask
 * for a {@code Class} and get that type, without a cast.
 *
 * <p>It is not thread-safe, even though it looks immutable: evaluation consults the resolvers of
 * the {@link XPath} that compiled it, and those may have state.
 */
public interface XPathExpression {

    /**
     * Evaluates on a tree and returns the requested type.
     *
     * @param returnType one of the constants in {@link XPathConstants}
     */
    Object evaluate(Object item, QName returnType) throws XPathExpressionException;

    /** Same, as a string. */
    String evaluate(Object item) throws XPathExpressionException;

    /** Parses the document and evaluates. See the class note about the cost. */
    Object evaluate(InputSource source, QName returnType) throws XPathExpressionException;

    /** Same, as a string. */
    String evaluate(InputSource source) throws XPathExpressionException;

    /**
     * The modern way: you ask for a type and you get that type.
     *
     * <p>By default it delegates to {@link #evaluate(Object, QName)}, translating the {@code Class}
     * to its corresponding {@code QName}.
     *
     * @throws IllegalArgumentException if that type is not one XPath can produce
     */
    default <T> T evaluateExpression(Object item, Class<T> type) throws XPathExpressionException {
        QName qname = XPathEvaluationResult.XPathResultType.getQNameType(type);
        if (qname == null) {
            throw new IllegalArgumentException("The type is not supported: " + type);
        }
        return type.cast(evaluate(item, qname));
    }

    /**
     * Evaluates without saying which type is expected.
     *
     * @throws UnsupportedOperationException by default: without an implementation there is no way
     *     to know which type the expression produced, and returning something with a made-up type
     *     would be worse
     */
    default XPathEvaluationResult<?> evaluateExpression(Object item)
        throws XPathExpressionException {
        throw new UnsupportedOperationException("evaluateExpression(Object item)");
    }

    /** Parses the document and evaluates, with the requested type. */
    default <T> T evaluateExpression(InputSource source, Class<T> type)
        throws XPathExpressionException {
        QName qname = XPathEvaluationResult.XPathResultType.getQNameType(type);
        if (qname == null) {
            throw new IllegalArgumentException("The type is not supported: " + type);
        }
        return type.cast(evaluate(source, qname));
    }

    /**
     * Same, without saying the type.
     *
     * @throws UnsupportedOperationException by default
     */
    default XPathEvaluationResult<?> evaluateExpression(InputSource source)
        throws XPathExpressionException {
        throw new UnsupportedOperationException("evaluateExpression(InputSource source)");
    }
}
