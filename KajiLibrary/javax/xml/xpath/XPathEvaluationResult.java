package javax.xml.xpath;

import javax.xml.namespace.QName;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.xpath.XPathEvaluationResult -- un resultado que sabe de que tipo es.
 *
 * <p>Es lo que devuelve {@code evaluateExpression} cuando no se dice que tipo se espera. Trae el
 * valor y su tipo juntos, que es la unica forma de contestar sin obligar a quien pregunta a adivinar.
 *
 * <p>Existe porque la via vieja --{@code evaluate} con un {@link QName}-- obliga a decidir el tipo
 * <b>antes</b> de evaluar, y hay expresiones cuyo tipo depende del documento. Con esto se evalua
 * primero y se decide despues.
 */
public interface XPathEvaluationResult<T> {

    /** De que tipo es el valor. */
    XPathResultType type();

    /** El valor. */
    T value();

    /**
     * Los tipos que XPath puede producir, como enum.
     *
     * <p>Es la version moderna de las constantes de {@link XPathConstants}, que son {@code QName}.
     * Los dos juegos conviven y {@link #getQNameType} es el puente entre ellos.
     *
     * <p>{@link #ANY} no es un tipo de resultado sino un pedido: significa "el que salga". Por eso
     * no tiene {@code QName} y por eso es el primero del enum.
     */
    public static enum XPathResultType {

        /** Cualquiera; se usa al pedir, no al recibir. */
        ANY,

        /** Un booleano. */
        BOOLEAN,

        /** Un numero; en XPath 1.0 siempre un {@code double}. */
        NUMBER,

        /** Una cadena. */
        STRING,

        /** Un conjunto de nodos. */
        NODESET,

        /** Un solo nodo. */
        NODE;

        /**
         * El {@link QName} que corresponde a esa clase de Java.
         *
         * <p>El mapeo tiene dos sorpresas que conviene tener presentes:
         *
         * <ul>
         *   <li>cualquier {@link Number} da {@link XPathConstants#NUMBER}, asi que pedir
         *       {@code Integer.class} funciona aunque XPath 1.0 no tenga enteros;
         *   <li>{@code org.w3c.dom.NodeList} devuelve <b>null</b>, aunque sea justo el tipo que la
         *       via vieja usa para un conjunto de nodos. El conjunto de nodos moderno es
         *       {@link XPathNodes}, y esta tabla es la del API moderno.
         * </ul>
         *
         * @return null si esa clase no es un tipo de resultado de XPath
         */
        public static QName getQNameType(Class<?> clsType) {
            if (clsType == null) {
                return null;
            }
            if (Boolean.class.equals(clsType)) {
                return XPathConstants.BOOLEAN;
            }
            if (String.class.equals(clsType)) {
                return XPathConstants.STRING;
            }
            if (XPathNodes.class.equals(clsType)) {
                return XPathConstants.NODESET;
            }
            if (Node.class.equals(clsType)) {
                return XPathConstants.NODE;
            }
            // Cualquier numero, no solo Double; ver la nota del metodo.
            if (Number.class.isAssignableFrom(clsType)) {
                return XPathConstants.NUMBER;
            }
            return null;
        }
    }
}
