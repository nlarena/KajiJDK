package javax.xml.xpath;

import javax.xml.namespace.QName;
import org.xml.sax.InputSource;

/**
 * KajiLibrary's javax.xml.xpath.XPathExpression -- una expresion ya compilada.
 *
 * <p>Compilar cuesta y evaluar es barato, asi que una expresion que se aplica muchas veces se
 * compila una sola. Es la misma relacion que hay entre {@code Pattern} y {@code Matcher}, o entre un
 * esquema compilado y sus validadores.
 *
 * <p>Las dos familias de {@code evaluate} se diferencian en de donde sale el documento: las que
 * reciben {@code Object} trabajan sobre un arbol que ya existe, y las que reciben
 * {@link InputSource} <b>leen y analizan</b> el documento en cada llamada. La segunda es comoda y
 * cara: para evaluar varias expresiones sobre el mismo documento hay que analizarlo una vez y usar
 * la primera.
 *
 * <p>Las sobrecargas sin {@link QName} devuelven {@code String}, que es pedir
 * {@link XPathConstants#STRING}. Las {@code evaluateExpression} son la via moderna: se pide un
 * {@code Class} y se recibe ese tipo, sin cast.
 *
 * <p>No es segura entre hilos, aunque parezca inmutable: la evaluacion consulta los resolvedores del
 * {@link XPath} que la compilo, y esos pueden tener estado.
 */
public interface XPathExpression {

    /**
     * Evalua sobre un arbol y devuelve el tipo pedido.
     *
     * @param returnType una de las constantes de {@link XPathConstants}
     */
    Object evaluate(Object item, QName returnType) throws XPathExpressionException;

    /** Idem, como cadena. */
    String evaluate(Object item) throws XPathExpressionException;

    /** Analiza el documento y evalua. Ver la nota de la clase sobre el costo. */
    Object evaluate(InputSource source, QName returnType) throws XPathExpressionException;

    /** Idem, como cadena. */
    String evaluate(InputSource source) throws XPathExpressionException;

    /**
     * La via moderna: se pide un tipo y se recibe ese tipo.
     *
     * <p>Por omision delega en {@link #evaluate(Object, QName)} traduciendo el {@code Class} al
     * {@code QName} que le corresponde.
     *
     * @throws IllegalArgumentException si ese tipo no es uno de los que XPath sabe producir
     */
    default <T> T evaluateExpression(Object item, Class<T> type) throws XPathExpressionException {
        QName qname = XPathEvaluationResult.XPathResultType.getQNameType(type);
        if (qname == null) {
            throw new IllegalArgumentException("The type is not supported: " + type);
        }
        return type.cast(evaluate(item, qname));
    }

    /**
     * Evalua sin decir que tipo se espera.
     *
     * @throws UnsupportedOperationException por omision: sin implementacion no hay como saber que
     *     tipo produjo la expresion, y devolver algo con un tipo inventado seria peor
     */
    default XPathEvaluationResult<?> evaluateExpression(Object item)
        throws XPathExpressionException {
        throw new UnsupportedOperationException("evaluateExpression(Object item)");
    }

    /** Analiza el documento y evalua, con el tipo pedido. */
    default <T> T evaluateExpression(InputSource source, Class<T> type)
        throws XPathExpressionException {
        QName qname = XPathEvaluationResult.XPathResultType.getQNameType(type);
        if (qname == null) {
            throw new IllegalArgumentException("The type is not supported: " + type);
        }
        return type.cast(evaluate(source, qname));
    }

    /**
     * Idem, sin decir el tipo.
     *
     * @throws UnsupportedOperationException por omision
     */
    default XPathEvaluationResult<?> evaluateExpression(InputSource source)
        throws XPathExpressionException {
        throw new UnsupportedOperationException("evaluateExpression(InputSource source)");
    }
}
