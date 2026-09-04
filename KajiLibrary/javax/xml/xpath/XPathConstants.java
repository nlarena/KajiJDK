package javax.xml.xpath;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.xpath.XPathConstants -- los cinco tipos de resultado de XPath.
 *
 * <p>XPath 1.0 tiene exactamente cuatro tipos --booleano, numero, cadena y conjunto de nodos-- y la
 * plataforma agrega {@link #NODE} para el caso comodo de querer uno solo. Se piden por
 * {@link QName} y no por {@code Class} porque el API es de 2004 y los genericos recien llegaban; la
 * via moderna es {@code evaluateExpression} con un {@code Class}.
 *
 * <p>El espacio de nombres de los cinco es el de XSLT y no el de XPath. Es una rareza historica
 * --XPath salio como parte de XSLT-- y hay que respetarla: un {@code QName} armado a mano con el
 * espacio "correcto" no coincide con estos y la evaluacion falla.
 *
 * <p>Pedir un tipo distinto del que la expresion produce no es un error: XPath <b>convierte</b>. Una
 * expresion que devuelve un conjunto de nodos pedida como {@link #STRING} da el texto del primer
 * nodo, y pedida como {@link #BOOLEAN} da si el conjunto esta vacio. Eso ultimo es la fuente clasica
 * de confusion: {@code evaluate(expr, doc, BOOLEAN)} sobre {@code //nodo} contesta "existe alguno" y
 * no el contenido del nodo.
 */
public class XPathConstants {

    /** El espacio de nombres de los cinco tipos; es el de XSLT. Ver la nota de la clase. */
    private static final String NS = "http://www.w3.org/1999/XSL/Transform";

    /** Un numero. XPath 1.0 no distingue entero de flotante: todo es {@code double}. */
    public static final QName NUMBER = new QName(NS, "NUMBER");

    /** Una cadena. */
    public static final QName STRING = new QName(NS, "STRING");

    /** Un booleano. */
    public static final QName BOOLEAN = new QName(NS, "BOOLEAN");

    /** Un conjunto de nodos, que llega como {@code org.w3c.dom.NodeList}. */
    public static final QName NODESET = new QName(NS, "NODESET");

    /** Un solo nodo, o null si no hay ninguno. */
    public static final QName NODE = new QName(NS, "NODE");

    /** El modelo de objetos DOM, que es el unico que la plataforma trae. */
    public static final String DOM_OBJECT_MODEL = "http://java.sun.com/jaxp/xpath/dom";

    /** Privado: la clase es solo constantes. */
    private XPathConstants() {
    }
}
