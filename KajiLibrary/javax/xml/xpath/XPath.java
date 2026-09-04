package javax.xml.xpath;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import org.xml.sax.InputSource;

/**
 * KajiLibrary's javax.xml.xpath.XPath -- el evaluador de expresiones.
 *
 * <p>Se obtiene de un {@link XPathFactory} y lleva tres piezas de contexto que la expresion puede
 * necesitar: los espacios de nombres, las variables y las funciones propias.
 *
 * <h2>El contexto de espacios de nombres no es opcional</h2>
 *
 * <p>Es el que mas problemas da. Una expresion como {@code /libro/titulo} <b>no encuentra nada</b> en
 * un documento cuyos elementos estan en un espacio de nombres, aunque en el texto del XML se vean
 * exactamente asi. XPath 1.0 no tiene concepto de espacio por omision: un nombre sin prefijo busca
 * elementos sin espacio de nombres, punto.
 *
 * <p>La solucion es un {@link NamespaceContext} que ate un prefijo al espacio del documento y usar
 * ese prefijo en la expresion. El prefijo no tiene por que ser el mismo que el del documento -- lo
 * que ata es el URI.
 *
 * <h2>Compilar o evaluar directo</h2>
 *
 * <p>{@link #compile} devuelve una {@link XPathExpression} reusable y los {@code evaluate} de aca
 * hacen las dos cosas de una. Para una expresion que se aplica muchas veces, compilar una vez es la
 * diferencia; para una sola, no vale la pena.
 *
 * <p>{@link #reset} devuelve el objeto al estado inicial <b>sin</b> volver a pedirlo a la fabrica: es
 * para reusarlo, no para deshacer una evaluacion.
 *
 * <p>No es seguro entre hilos.
 */
public interface XPath {

    /** Vuelve al estado inicial: sin resolvedores y sin contexto de nombres. */
    void reset();

    /** Quien resuelve las variables {@code $nombre}; ver {@link XPathVariableResolver}. */
    void setXPathVariableResolver(XPathVariableResolver resolver);

    /** Ver {@link #setXPathVariableResolver}. */
    XPathVariableResolver getXPathVariableResolver();

    /** Quien resuelve las funciones propias. */
    void setXPathFunctionResolver(XPathFunctionResolver resolver);

    /** Ver {@link #setXPathFunctionResolver}. */
    XPathFunctionResolver getXPathFunctionResolver();

    /** Los prefijos de espacio de nombres. Ver la nota de la clase: casi siempre hace falta. */
    void setNamespaceContext(NamespaceContext nsContext);

    /** Ver {@link #setNamespaceContext}. */
    NamespaceContext getNamespaceContext();

    /**
     * Compila una expresion para reusarla.
     *
     * @throws XPathExpressionException si la sintaxis esta mal
     */
    XPathExpression compile(String expression) throws XPathExpressionException;

    /**
     * Compila y evalua de una, devolviendo el tipo pedido.
     *
     * @param returnType una de las constantes de {@link XPathConstants}
     */
    Object evaluate(String expression, Object item, QName returnType)
        throws XPathExpressionException;

    /** Idem, como cadena. */
    String evaluate(String expression, Object item) throws XPathExpressionException;

    /** Analiza el documento, compila y evalua. */
    Object evaluate(String expression, InputSource source, QName returnType)
        throws XPathExpressionException;

    /** Idem, como cadena. */
    String evaluate(String expression, InputSource source) throws XPathExpressionException;

    /**
     * La via moderna: se pide un tipo y se recibe ese tipo.
     *
     * @throws IllegalArgumentException si ese tipo no es uno de los que XPath sabe producir
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
     * Evalua sin decir que tipo se espera.
     *
     * @throws UnsupportedOperationException por omision; ver
     *     {@link XPathExpression#evaluateExpression(Object)}
     */
    default XPathEvaluationResult<?> evaluateExpression(String expression, Object item)
        throws XPathExpressionException {
        throw new UnsupportedOperationException("evaluateExpression(String expression, Object item)");
    }

    /** Analiza el documento y evalua, con el tipo pedido. */
    default <T> T evaluateExpression(String expression, InputSource source, Class<T> type)
        throws XPathExpressionException {
        QName qname = XPathEvaluationResult.XPathResultType.getQNameType(type);
        if (qname == null) {
            throw new IllegalArgumentException("The type is not supported: " + type);
        }
        return type.cast(evaluate(expression, source, qname));
    }

    /**
     * Idem, sin decir el tipo.
     *
     * @throws UnsupportedOperationException por omision
     */
    default XPathEvaluationResult<?> evaluateExpression(String expression, InputSource source)
        throws XPathExpressionException {
        throw new UnsupportedOperationException(
            "evaluateExpression(String expression, InputSource source)");
    }
}
