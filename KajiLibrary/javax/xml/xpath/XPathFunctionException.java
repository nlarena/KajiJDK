package javax.xml.xpath;

/**
 * KajiLibrary's javax.xml.xpath.XPathFunctionException -- fallo una funcion de extension.
 *
 * <p>Es la unica excepcion de este paquete que la lanza <b>quien usa</b> el API y no la
 * implementacion: sale de un {@link XPathFunction} que uno escribio. Por eso hereda de
 * {@link XPathExpressionException} -- desde afuera, una funcion que falla es una evaluacion que
 * falla, y quien llamo a {@code evaluate} no tiene por que saber que habia una funcion propia
 * adentro.
 */
public class XPathFunctionException extends XPathExpressionException {

    private static final long serialVersionUID = -1837080260374986980L;

    /** Con un mensaje. */
    public XPathFunctionException(String message) {
        super(message);
    }

    /** Con la causa de abajo. */
    public XPathFunctionException(Throwable cause) {
        super(cause);
    }
}
