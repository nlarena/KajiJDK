package javax.xml.xpath;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import org.xml.sax.InputSource;

/**
 * KajiLibrary's javax.xml.xpath.XPath -- the expression evaluator.
 *
 * <p>It is obtained from an {@link XPathFactory} and carries three pieces of context the expression
 * may need: the namespaces, the variables and the custom functions.
 *
 * <h2>The namespace context is not optional</h2>
 *
 * <p>It is the one that causes the most trouble. An expression like {@code /book/title} <b>finds
 * nothing</b> in a document whose elements are in a namespace, even if the XML text looks exactly
 * like that. XPath 1.0 has no concept of a default namespace: an unprefixed name looks for elements
 * with no namespace, period.
 *
 * <p>The fix is a {@link NamespaceContext} that binds a prefix to the document's namespace, and
 * using that prefix in the expression. The prefix does not have to be the same as the document's --
 * what binds is the URI.
 *
 * <h2>Compile or evaluate directly</h2>
 *
 * <p>{@link #compile} returns a reusable {@link XPathExpression}, and the {@code evaluate} methods
 * here do both at once. For an expression applied many times, compiling once makes the difference;
 * for a single one, it is not worth it.
 *
 * <p>{@link #reset} returns the object to its initial state <b>without</b> asking the factory for
 * it again: it is for reusing it, not for undoing an evaluation.
 *
 * <p>It is not thread-safe.
 */
public interface XPath {

    /** Back to the initial state: no resolvers and no namespace context. */
    void reset();

    /** Who resolves the {@code $name} variables; see {@link XPathVariableResolver}. */
    void setXPathVariableResolver(XPathVariableResolver resolver);

    /** Ver {@link #setXPathVariableResolver}. */
    XPathVariableResolver getXPathVariableResolver();

    /** Who resolves the custom functions. */
    void setXPathFunctionResolver(XPathFunctionResolver resolver);

    /** Ver {@link #setXPathFunctionResolver}. */
    XPathFunctionResolver getXPathFunctionResolver();

    /** The namespace prefixes. See the class note: it is almost always needed. */
    void setNamespaceContext(NamespaceContext nsContext);

    /** Ver {@link #setNamespaceContext}. */
    NamespaceContext getNamespaceContext();

    /**
     * Compiles an expression for reuse.
     *
     * @throws XPathExpressionException if the syntax is wrong
     */
    XPathExpression compile(String expression) throws XPathExpressionException;

    /**
     * Compiles and evaluates at once, returning the requested type.
     *
     * @param returnType one of the constants in {@link XPathConstants}
     */
    Object evaluate(String expression, Object item, QName returnType)
        throws XPathExpressionException;

    /** Same, as a string. */
    String evaluate(String expression, Object item) throws XPathExpressionException;

    /** Parses the document, compiles and evaluates. */
    Object evaluate(String expression, InputSource source, QName returnType)
        throws XPathExpressionException;

    /** Same, as a string. */
    String evaluate(String expression, InputSource source) throws XPathExpressionException;

    /**
     * The modern way: you ask for a type and you get that type.
     *
     * @throws IllegalArgumentException if that type is not one XPath can produce
     */
    default <T> T evaluateExpression(String expression, Object item, Class<T> type)
        throws XPathExpressionException {
        QName qname = XPathEvaluationResult.XPathResultType.getQNameType(type);
        if (qname == null) {
            throw new IllegalArgumentException("The type is not supported: " + type);
        }
        return type.cast(evaluate(expression, item, qname));
    }

    /**
     * Evaluates without saying which type is expected.
     *
     * @throws UnsupportedOperationException by default; see
     *     {@link XPathExpression#evaluateExpression(Object)}
     */
    default XPathEvaluationResult<?> evaluateExpression(String expression, Object item)
        throws XPathExpressionException {
        throw new UnsupportedOperationException("evaluateExpression(String expression, Object item)");
    }

    /** Parses the document and evaluates, with the requested type. */
    default <T> T evaluateExpression(String expression, InputSource source, Class<T> type)
        throws XPathExpressionException {
        QName qname = XPathEvaluationResult.XPathResultType.getQNameType(type);
        if (qname == null) {
            throw new IllegalArgumentException("The type is not supported: " + type);
        }
        return type.cast(evaluate(expression, source, qname));
    }

    /**
     * Same, without saying the type.
     *
     * @throws UnsupportedOperationException by default
     */
    default XPathEvaluationResult<?> evaluateExpression(String expression, InputSource source)
        throws XPathExpressionException {
        throw new UnsupportedOperationException(
            "evaluateExpression(String expression, InputSource source)");
    }
}
